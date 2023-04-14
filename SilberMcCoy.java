import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Array;
import java.util.*;

public class SilberMcCoy {
    private IDictionary dict;
    private ArrayList<IWord>[] metachains;
    private int numChains;

    // for each word, track which chains it currently belongs to
    private HashMap<String, List<Integer>> wordChains;
    public SilberMcCoy(String inputFile) {
        int WN_SIZE = 117798;
        wordChains = new HashMap<>();
        numChains = 0;

        try {       // open WordNet dictionary
            String wnhome = "./WordNet-3.0";
            String path = wnhome + File.separator + "dict";
            URL url = new URL("file", null, path);

            dict = new Dictionary(url);
            dict.open();
        } catch (IOException e) {
            System.out.println("ERR: " + e.getMessage());
        }

        List<String> fileWords = new ArrayList<String>();
        try {       // scan from POS-tagged file
            Scanner sc = new Scanner(new File(inputFile));
            while (sc.hasNext()) {
                fileWords.add(sc.next());
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERR: " + e.getMessage());
        }

        // initialize metachains array, given size
        metachains = new ArrayList[fileWords.size() + WN_SIZE];
        for (int i = 0; i < metachains.length; i++) {
            metachains[i] = new ArrayList<IWord>();
        }

        for (String s : fileWords) {
            if (isNoun(s)) {
                String noun = s.split("_")[0];
                IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);

                // TODO: update wordChains references
                if (idxWord != null) {
                    List<IWordID> wordIDs = idxWord.getWordIDs();

                    for (IWordID i : wordIDs) {
                        int metaIndex = 0;
                        IWord word = dict.getWord(i);
                        ISenseKey key = word.getSenseKey();
                        // System.out.println(key.getLexicalID() + " " + key.getLemma() + "- " + word.getSynset().getGloss());

                        // look for chain starting at metaIndex
                        // chains are added in order, if we reach a chain of size zero, it's a new sense
                        searchChainSense:
                        while (true) {
                            boolean alrExists = false;
                            if (!alrExists && metachains[metaIndex].size() != 0) {    // check existing chain
                                ISenseKey chainSense = metachains[metaIndex].get(0).getSenseKey();
//                                System.out.println(chainSense.getLexicalID() + " " + key.getLexicalID());
//                                System.out.println(chainSense.getLemma() + " " + key.getLemma());
//                                System.out.println(metaIndex + ": " + chainSense.getLemma() + "/" + key.getLemma());
//                                System.out.println(metaIndex + ": " + metachains[metaIndex].get(0).getSynset().getID() + "/" + word.getSynset().getID());
                                if (compareSynsets(metachains[metaIndex].get(0), word)) {
                                    if (placeWordInChain(metaIndex, word)) {
                                        break;
                                    } else {
                                        alrExists = true;
                                        break;
                                    }
                                }
                            } else {
                                numChains++;
                                metachains[metaIndex].add(word);
                                break;
                            }
                            metaIndex++;
                        }
                    }
                }
            }
        }
    }

    private boolean compareSynsets(IWord chainHead, IWord word) {
        return chainHead.getSynset().getID().equals(word.getSynset().getID());
    }

    // check if a word exists in this chain sense
    private boolean placeWordInChain(int i, IWord word) {
        String wordtxt = word.getLemma();
        boolean placed = false;
        if (!metachains[i].contains(word)) {
            // System.out.println("Adding to existing chain: " + word.getLemma());
            metachains[i].add(word);
            if (wordChains.containsKey(wordtxt)) {
                wordChains.get(wordtxt).add(i);
            } else {
                List<Integer> inChains = new ArrayList<>();
                inChains.add(i);
                wordChains.put(wordtxt, inChains);
            }
            placed = true;
        }
        return placed;
    }

    private boolean isSynonym(IWord chainWord, IWord word) {
        ISynset checkSet = chainWord.getSynset();
        for (IWord w: checkSet.getWords()) {
            if (w.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHypernym(IWord chainWord, IWord word) {
        ISynset checkSet = chainWord.getSynset();
        List<ISynsetID> hypernyms = checkSet.getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID sid: hypernyms) {
            for (IWord w: dict.getSynset(sid).getWords()) {
                if (w.equals(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(numChains + " METACHAINS: \n");
        for (int i = 0; i < metachains.length; i++) {
            if (metachains[i].size() != 0) {
                sb.append("{");
                for (IWord word: metachains[i]) {
                    sb.append(word.getLexicalID()+ "-" +word.getLemma() +"-"+word.getSynset().getID()+",");
                }
                sb.deleteCharAt(sb.length()-1);
                sb.append("}\n");
            }
        }
        return sb.toString();
    }

    // check POS tag of word
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

    public static void main(String[] args) {
        SilberMcCoy test = new SilberMcCoy("synonyms_tagged.txt");
        System.out.println(test);
    }
}

