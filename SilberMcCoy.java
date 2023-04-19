import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import javax.sound.midi.MetaEventListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/*
    POS tagger command: ./stanford-postagger.sh models/english-bidirectional-distsim.tagger nyc_rat.txt > nyc_rat_tagged.txt
 */
public class SilberMcCoy {
    private IDictionary dict;
    private ArrayList<Metachain> metachains;
    public ArrayList<WordInstance> nouns;

    public SilberMcCoy(String inputFile) {
        initializeAlgo();
        // First pass through document: construct metachains
        Scanner sc = openScanner(inputFile);
        System.out.println("Begin");
        constructMetachains(sc);

        System.out.println("FINISHED CONSTRUCTION");
        System.out.println(this);
        System.out.println();

        // Second pass *through nouns list*: chain culling
        for (WordInstance inst: nouns) {
            for (MetaWordNode metaNode: inst.getMetaInstances()) {
                if (metaNode != inst.getBestContribution()) {
                    double score = metaNode.score;
                    Metachain c = metachains.get(metaNode.chainNum);
                    c.strengthScore = c.strengthScore - score;
                    metaNode.score = 0.0;
                }
            }
        }

        System.out.println("FINISHED CULLING");
        System.out.println(nonZeroChains());
        System.out.println("\n");

//        for (WordInstance wi: nouns) {
//            System.out.println(wi);
//        }

        System.out.println("Strong Chains: ");
        List<Metachain> strongChains = getStrongChains();
        for (Metachain c: strongChains) {
            System.out.println(c);
        }
    }

    private void initializeAlgo() {
        metachains = new ArrayList<>();
        nouns = new ArrayList<>();
        openDict();
    }

    private Scanner openScanner(String inputFile) {
        try {
            Scanner sc = new Scanner(new File(inputFile));
            return sc;
        } catch (FileNotFoundException e) {
            System.out.println("ERR: " + e.getMessage());
        }
        return null;
    }

    private void constructMetachains(Scanner sc) {
        System.out.println("Begin scanning file");
        int sentNum = 1;
        while (sc.hasNext()) {
            String str = sc.next();

            if (isNoun(str)) {
                String noun;
                String[] split = str.split("_");
                if (split.length == 2) {
                    noun = split[0];
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < split.length - 1; i++) {
                        sb.append(split[i] + " ");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    noun = sb.toString();
                }
                IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);

                if (idxWord != null) {
                    List<IWordID> wordSenses = idxWord.getWordIDs();
                    WordInstance instance = new WordInstance(idxWord, sentNum);

                    // for each sense of this noun, insert into as many chains as possible
                    for (IWordID id : wordSenses) {
                        IWord word = dict.getWord(id);
                        insertSenseIntoChains(instance, word, sentNum);
                    }
                    nouns.add(instance);
                }
            } else if (isPunctuation(str)) {
                sentNum++;
            }
        }
    }

    private boolean nodeExists(MetaWordNode node) {
        return node != null;
    }

    private void openDict() {
        try {
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
        String[] split = s.split("_");
        String tag = split[split.length - 1];

        if (tag.equals("NN") || tag.equals("NNS") || tag.equals("NNP") || tag.equals("NNPS")) {
            return true;
        }
        return false;
    }

    private boolean isProperNoun(String s) {
        String[] split = s.split("_");
        String tag = split[split.length - 1];

        if (tag.equals("NNP") || tag.equals("NNPS")) {
            return true;
        }
        return false;
    }

    private boolean isPunctuation(String str) {
        return str.matches(".*_.");
    }

    private void insertSenseIntoChains(WordInstance parent, IWord word, int sentNum) {
        MetaWordNode newNode = new MetaWordNode(sentNum, word);
        boolean inserted = false;

        for (int i = 0; i < metachains.size(); i++) {
            Metachain c = metachains.get(i);
            ISynsetID currID = word.getSynset().getID();

            // Check relation with senses of existing chains
            if (isSynonym(c.headSense, currID)) {
                inserted = true;
                insertIntoChain(parent, newNode, i, c);
            } else if (isHypernym(c.headSense, currID)) {
                insertIntoChain(parent, newNode, i, c);
            }
        }
        // Create a new chain for this sense, if not found
        if (!inserted) {
            MetaWordNode startNewChain = new MetaWordNode(sentNum, word);
            createNewChain(startNewChain);
            parent.addMetaInstance(startNewChain);
            updateBest(parent, startNewChain);
        }
    }

    private void insertIntoChain(WordInstance parent, MetaWordNode node, int chainIndex, Metachain c) {
        node.chainNum = chainIndex;

        // find nearest node + calculate relation score
        for (int i = c.getSize() - 1; i >= 0; i--) {
            MetaWordNode currChainNode = c.chain.get(i);
            int dist = currChainNode.computeDist(node);
            double currScore = computeSMCoyScore(currChainNode, node, dist);

            if (currScore != Double.NEGATIVE_INFINITY) {
                node.score = currScore;

                c.insertWord(node, currScore);
                parent.addMetaInstance(node);
                updateBest(parent, node);
                break;
            }
        }
    }

    private void updateBest(WordInstance parent, MetaWordNode node) {
        MetaWordNode currBest = parent.getBestContribution();
        if (currBest == null) {
            parent.setBestNode(node);
        } else if (currBest.score < node.score) {
            parent.setBestNode(node);
        } else if (currBest.score == node.score) {
            ISynsetID curr = currBest.word.getSynset().getID();
            ISynsetID compare = node.word.getSynset().getID();
            if (curr.toString().compareTo(compare.toString()) < 0) {
                parent.setBestNode(node);
            }
        }
    }

    private boolean isSynonym(ISynsetID s1, ISynsetID s2) {
        return s1.equals(s2);
    }

    private boolean isHypernym(ISynsetID chain, ISynsetID node) {
        List<ISynsetID> hypernyms = dict.getSynset(chain).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h: hypernyms) {
            if (h.equals(node)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHyperHypo(ISynsetID s1, ISynsetID s2) {
        isHypernym(s1, s2);

        List<ISynsetID> hyponyms = dict.getSynset(s1).getRelatedSynsets(Pointer.HYPONYM);
        for (ISynsetID h: hyponyms) {
            if (h.equals(s2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSibling(ISynsetID s1, ISynsetID s2) {
        List<ISynsetID> hypernym1 = dict.getSynset(s1).getRelatedSynsets(Pointer.HYPERNYM);
        List<ISynsetID> hypernym2 = dict.getSynset(s2).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID id1 : hypernym1) {
            for (ISynsetID id2 : hypernym2) {
                if (id1.equals(id2)) {
                    // System.out.println(id1.toString() + "-" + id2);
                    return true;
                }
            }
        }
        return false;
    }

    private double computeSMCoyScore(MetaWordNode chainNode, MetaWordNode newNode, int dist) {
        double factor;
        // 1. Identity or Synonym Relation
        if (chainNode.isSynonym(newNode.word.getSynset().getID())) {
            if (dist <= 3) {
                factor = 1.0;
            } else {
                factor = 0.0;
            }
            return factor;
            // return factor*length + chainNode.score;
            // 2. Hypernym Relation
        } else if (chainNode.isHyperHypo(dict, newNode.word.getSynset().getID())){
            if (dist <= 1) {
                factor = 1.0;
            } else if (dist <= 3) {
                factor = 0.5;
            } else {
                factor = 0.0;
            }
            // return factor*length + chainNode.score;
            return factor;
        } else if (chainNode.isSibling(dict, newNode.word.getSynset().getID())) {
            if (dist <= 1) {
                factor = 1.0;
            } else if (dist <= 3) {
                factor = 0.3;
            } else {
                factor = 0.0;
            }
            // return factor*length + chainNode.score;
            return factor;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private void createNewChain(MetaWordNode curr) {
        Metachain newChain = new Metachain(curr);
        curr.score = 1.0;
        curr.chainNum = metachains.size();
        metachains.add(newChain);
    }

    private MetaWordNode getStrongerNode(MetaWordNode currBest, MetaWordNode check) {
        if (!nodeExists(currBest)) {
            return check;
        } else {
            if (currBest.score < check.score) {
                return check;
            }
            return currBest;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(metachains.size() + " METACHAINS: \n");
        for (Metachain c: metachains) {
            sb.append(c.toString() + "\n");
        }
        return sb.toString();
    }

    public String nonZeroChains() {
        StringBuilder sb = new StringBuilder();
        int numChains = 0;

        for (Metachain c: metachains) {
            if (c.strengthScore != 0.0) {
                numChains++;
                sb.append(c.toString() + "\n");
            }
        }
        sb.append(numChains + " METACHAINS: \n");
        return sb.toString();
    }

    public List<Metachain> getStrongChains() {
        List<Metachain> strong = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        // this is done post culling, lots of zeroes
        for (Metachain c: metachains) {
            scores.add(c.strengthScore);
        }

        double stdev = getStandardDeviation(scores);
        double mean = getMean(scores);
        for (int i = 0; i < metachains.size(); i++) {
            if (metachains.get(i).strengthScore - mean > 2*stdev) {
                strong.add(metachains.get(i));
            }
        }

        return strong;
    }

    private double getStandardDeviation(List<Double> scores) {
        double mean = getMean(scores);
        double result = 0.0;
        for (double s: scores) {
            result += Math.pow((s - mean), 2);
        }
        return Math.sqrt(result/scores.size());
    }

    private double getMean(List<Double> scores) {
        return scores.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(-1.0);
    }

    public static void main(String[] args) {
        SilberMcCoy test = new SilberMcCoy("fox_dominion_tagged.txt");
        System.out.println();
        System.out.println("Nouns: " + test.nouns.size());
    }
}
