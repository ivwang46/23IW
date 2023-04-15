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

                if (idxWord != null) {
                    List<IWordID> wordIDs = idxWord.getWordIDs();

                    for (IWordID i : wordIDs) {
                        int metaIndex = 0;
                        IWord word = dict.getWord(i);
                        ISenseKey key = word.getSenseKey();
                        // System.out.println(key.getLexicalID() + " " + key.getLemma() + "- " + word.getSynset().getGloss());

                        // look for chain starting at metaIndex
                        // chains are added in order, if we reach a chain of size zero, it's a new sense

                        boolean placed = false;

                        while (true) {  // search all existing chains
                            if (metachains[metaIndex].size() != 0) {    // check existing chain
                                IWord chainHead = metachains[metaIndex].get(0);
                                ISenseKey chainSense = metachains[metaIndex].get(0).getSenseKey();
//                                System.out.println(chainSense.getLexicalID() + " " + key.getLexicalID());
//                                System.out.println(chainSense.getLemma() + " " + key.getLemma());
//                                System.out.println(metaIndex + ": " + chainSense.getLemma() + "/" + key.getLemma());
//                                System.out.println(metaIndex + ": " + metachains[metaIndex].get(0).getSynset().getID() + "/" + word.getSynset().getID());

                                if (isSynonym(chainHead, word) || isHypernym(chainHead, word)) {
                                    placeWordInChain(metaIndex, word);
                                    placed = true;
                                    break;
                                }
                            } else {
                                numChains++;
                                metachains[metaIndex].add(word);
                                break;
                            }
                            metaIndex++;
                        }

                        if (!placed) {      // could not be placed into existing chain, create new
                            numChains++;
                            metachains[metaIndex].add(word);

                        }
                    }
                }
            }
        }
    }

    private void updateWordChains(int i, IWord word) {
        String wordtxt = word.getLemma();
        if (!wordChains.containsKey(wordtxt)) {
            List<Integer> inChains = new ArrayList<>();
            inChains.add(i);
            wordChains.put(wordtxt, inChains);
        } else if (!wordChains.get(wordtxt).contains(i)){
            wordChains.get(wordtxt).add(i);
        }
    }

    // check if a word exists in this chain sense
    // could check for chain index in HashMap instead
    private void placeWordInChain(int i, IWord word) {
        metachains[i].add(word);
        updateWordChains(i, word);
    }

    private boolean isSynonym(IWord chainWord, IWord word) {
        return chainWord.getSynset().getID() == word.getSynset().getID();
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

