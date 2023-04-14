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
    private ArrayList<IWord>[] metachains;

    // for each word, track which chains it currently belongs to
    private HashMap<String, ArrayList<Integer>> wordChains;
    public SilberMcCoy(String inputFile) {
        int WN_SIZE = 117798;
        try {       // open WordNet dictionary
            String wnhome = "./WordNet-3.0";
            String path = wnhome + File.separator + "dict";
            URL url = new URL("file", null, path);

            dict = new Dictionary(url);
            dict.open();
            System.out.println("Dictionary success");
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
                    int metaIndex = 0;

                    for (IWordID i : wordIDs) {
                        IWord word = dict.getWord(i);
                        ISenseKey key = word.getSenseKey();
                        // System.out.println(key.getLexicalID() + " " + key.getLemma() + "- " + word.getSynset().getGloss());

                        // look for chain starting at metaIndex
                        // chains are added in order, if we reach a chain of size zero, it's a new sense
                        while (true) {
                            if (metachains[metaIndex].size() != 0) {
                                ISenseKey chainSense = metachains[metaIndex].get(0).getSenseKey();
//                                System.out.println(chainSense.getLexicalID() + " " + key.getLexicalID());
//                                System.out.println(chainSense.getLemma() + " " + key.getLemma());
                                if (chainSense.getLexicalID() == key.getLexicalID()
                                        && chainSense.getLemma().equals(key.getLemma())) {
                                    metachains[metaIndex].add(word);
                                    break;
                                }
                            } else {
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
        sb.append("METACHAINS: \n");
        for (int i = 0; i < metachains.length; i++) {
            if (metachains[i].size() != 0) {
                sb.append("{");
                for (IWord word: metachains[i]) {
                    sb.append(word.getLemma() +",");
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
        SilberMcCoy test = new SilberMcCoy("longing_tagged.txt");
        System.out.println(test);
    }
}

