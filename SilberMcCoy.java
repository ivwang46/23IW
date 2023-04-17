import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class SilberMcCoy {
    private IDictionary dict;
    private final ArrayList<Metachain> metachains;
    private int numChains;
    private Map<WordNode, List<Integer>> nodeChains;

    public SilberMcCoy(String inputFile) {
        numChains = 0;
        metachains = new ArrayList<>();
        nodeChains = new HashMap<>();
        openDict();

        int sentNum = 1;    // starting at the first sentence
        try {
            Scanner sc = new Scanner(new File(inputFile));
            while (sc.hasNext()) {
                String str = sc.next();
                if (isNoun(str)) {
                    String noun = str.split("_")[0];
                    IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);
                    if (idxWord != null) {
                        List<IWordID> wordSenses = idxWord.getWordIDs();

                        for (IWordID id : wordSenses) {
                            IWord word = dict.getWord(id);
                            WordNode currNode = new WordNode(sentNum, word);
                            if (!addNodeToChains(currNode)) {
                                createNewChain(numChains, currNode);
                            }
                        }
                    }
                } else if (isPunctuation(str)) {
                    sentNum++;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERR: " + e.getMessage());
        }
    }

    private void openDict() {
        try {       // open WordNet dictionary
            String wnhome = "./WordNet-3.0";
            String path = wnhome + File.separator + "dict";
            URL url = new URL("file", null, path);

            dict = new Dictionary(url);
            dict.open();
        } catch (IOException e) {
            System.out.println("ERR: " + e.getMessage());
        }
    }

    private boolean isNoun(String s) {
        String[] nounTags = {"NN", "NNS", "NNP", "NNPS"};
        String[] split = s.split("_");
        String tag = split[split.length - 1];

        for (String t: nounTags) {
            if (tag.equals(t)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPunctuation(String str) {
        return str.matches(".*_.");
    }

    // iterate through all existing chains, insert new node where it fits
    // start a new chain unless exact synset has been found
    private boolean addNodeToChains(WordNode curr) {
        boolean found = false;
        for (int i = 0; i < numChains; i++) {
            Metachain c = metachains.get(i);

            String relation = "";
            // first check WordNode for relation to sense
            if (curr.isSynonym(c.headSense) || curr.isHyperHypo(dict, c.headSense)) {
                // find the NEAREST-IN-TEXT related word
                for (int n = c.getSize() - 1; n >= 0; n--) {
                    double currScore = Double.NEGATIVE_INFINITY;
                    WordNode chainNode = c.chain.get(n);
                    int dist = chainNode.computeDist(curr);

                    // 1. Identity or Synonym Relation
                    if (chainNode.isSynonym(c.headSense)) {
                        found = true;
                        if (dist <= 3) {
                            currScore = 1.0;
                        } else {
                            currScore = 0.0;
                        }
                        // 2. Hypernym Relation
                    } else if (chainNode.isHyperHypo(dict, c.headSense)){
                        found = true;
                        if (dist <= 3) {
                            if (dist <= 1) {
                                currScore = 1.0;
                            } else {
                                currScore = 0.5;
                            }
                        } else {
                            currScore = 0.0;
                        }
                    }

                    if (currScore != Double.NEGATIVE_INFINITY) {
                        c.insertWord(chainNode, curr, currScore);
                    }
                }
            }
        }
        return found;
    }

    private void createNewChain(int metaIndex, WordNode curr) {
        Metachain newChain = new Metachain(curr);
        metachains.add(newChain);
        numChains++;
        List<Integer> newList = new ArrayList<>();
        newList.add(metaIndex);
        nodeChains.put(curr, newList);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(numChains + " METACHAINS: \n");
        for (Metachain c: metachains) {
            sb.append(c.toString() + "\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SilberMcCoy test = new SilberMcCoy("paper_ex_tagged.txt");
        System.out.println(test);
    }
}
