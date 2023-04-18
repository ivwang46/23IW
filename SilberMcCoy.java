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
    private Map<MetaWordNode, List<Integer>> nodeChains;
    public ArrayList<WordInstance> nouns;

    public SilberMcCoy(String inputFile) {
        numChains = 0;
        metachains = new ArrayList<>();
        nodeChains = new HashMap<>();
        nouns = new ArrayList<>();
        openDict();

        int sentNum = 1;    // starting at the first sentence, construct metachains
        try {
            Scanner sc = new Scanner(new File(inputFile));
            while (sc.hasNext()) {
                String str = sc.next();

                if (isNoun(str)) {
                    String noun = str.split("_")[0];
                    IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);

                    if (idxWord != null) {
                        List<IWordID> wordSenses = idxWord.getWordIDs();
                        WordInstance instance = new WordInstance(idxWord, sentNum);

                        // for each sense of this noun, insert into as many chains as possible
                        MetaWordNode bestNode = null;
                        for (IWordID id : wordSenses) {
                            IWord word = dict.getWord(id);
                            MetaWordNode currNode = addNodeToChains(instance, word, sentNum);

                            if (nodeExists(currNode)) {
                                bestNode = getStrongerNode(bestNode, currNode);
                            } else {    // word sense does not belong in existing chains, create new chain
                                createNewChain(new MetaWordNode(sentNum, word));
                            }
                        }

                        instance.setBestNode(bestNode);
                        nouns.add(instance);
                    }
                } else if (isPunctuation(str)) {
                    sentNum++;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERR: " + e.getMessage());
        }

        // chain culling
        try {
            Scanner sc = new Scanner(new File(inputFile));
        } catch (FileNotFoundException e) {
            System.out.println("ERR: " + e.getMessage());
        }
    }

    private boolean nodeExists(MetaWordNode node) {
        return node != null;
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
    // returns {bestScore, bestChainIndex} or {Double.NEGATIVE_INFINITY, -1}
    private MetaWordNode addNodeToChains(WordInstance parent, IWord word, int sentNum) {
        MetaWordNode best = null;
        for (int i = 0; i < numChains; i++) {
            Metachain c = metachains.get(i);
            ISynsetID currID = word.getSynset().getID();

            // First, check for sense relations
            if (isSynonym(c.headSense, currID) || isHyperHypo(c.headSense, currID)) {
                MetaWordNode newNode = new MetaWordNode(sentNum, word);

                // find the NEAREST-IN-TEXT related word and compute the score
                for (int n = c.getSize() - 1; n >= 0; n--) {
                    MetaWordNode chainNode = c.chain.get(n);
                    int dist = chainNode.computeDist(newNode);

                    double currScore = computeRelationScore(chainNode, c.headSense, dist);
                    if (currScore != Double.NEGATIVE_INFINITY) {    // belongs to chain
                        newNode.score = currScore;
                        c.insertWord(chainNode, newNode, currScore);
                        parent.addMetaInstance(newNode);
                        // check score
                        if (best == null || best.score < currScore) {
                            best = newNode;
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    private boolean isSynonym(ISynsetID s1, ISynsetID s2) {
        return s1.equals(s2);
    }

    private boolean isHyperHypo(ISynsetID s1, ISynsetID s2) {
        List<ISynsetID> hypernyms = dict.getSynset(s1).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h: hypernyms) {
            if (h.equals(s2)) {
                return true;
            }
        }

        List<ISynsetID> hyponyms = dict.getSynset(s1).getRelatedSynsets(Pointer.HYPONYM);
        for (ISynsetID h: hyponyms) {
            if (h.equals(s2)) {
                return true;
            }
        }
        return false;
    }

    private double computeRelationScore(MetaWordNode chainNode, ISynsetID headSense, int dist) {
        // 1. Identity or Synonym Relation
        if (chainNode.isSynonym(headSense)) {
            if (dist <= 3) {
                return 1.0;
            } else {
                return 0.0;
            }
            // 2. Hypernym Relation
        } else if (chainNode.isHyperHypo(dict, headSense)){
            if (dist <= 3) {
                if (dist <= 1) {
                    return 1.0;
                } else {
                    return 0.5;
                }
            } else {
                return 0.0;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    private void createNewChain(MetaWordNode curr) {
        Metachain newChain = new Metachain(curr);
        metachains.add(newChain);
        List<Integer> newList = new ArrayList<>();
        newList.add(numChains);
        numChains++;
        nodeChains.put(curr, newList);
    }

    private MetaWordNode getStrongerNode(MetaWordNode currBest, MetaWordNode check) {
        if (!nodeExists(currBest)) {
            return check;
        } else {
            if (currBest.score < check.score) {
                return check;
            }
        }
        return currBest;
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
        SilberMcCoy test = new SilberMcCoy("test_3_tagged.txt");
        System.out.println(test);
        System.out.println();
        System.out.println("Nouns: " + test.nouns.size());
//        for (WordInstance wi: test.nouns) {
//            System.out.println(wi.toString());
//        }
    }
}
