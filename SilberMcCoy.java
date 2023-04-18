import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

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
    private int numChains;
    private Map<MetaWordNode, List<Integer>> nodeChains;
    public ArrayList<WordInstance> nouns;

    public SilberMcCoy(String inputFile) {
        initializeAlgo();
        // First pass through document: construct metachains
        Scanner sc = openScanner(inputFile);
        constructMetachains(sc);

        System.out.println("FINISHED CONSTRUCTION");

        // Second pass *through nouns list*: chain culling
        for (WordInstance inst: nouns) {
            for (MetaWordNode metaNode: inst.getMetaInstances()) {
                if (metaNode != inst.getBestContribution()) {
                    double score = metaNode.score;
                    metachains.get(metaNode.chainNum).strengthScore = metachains.get(metaNode.chainNum).strengthScore - score;
                    metaNode.score = 0.0;
                }
            }
        }
        System.out.println("FINISHED CULLING");
        System.out.println(nonZeroChains());
    }

    private void initializeAlgo() {
        numChains = 0;
        metachains = new ArrayList<>();
        nodeChains = new HashMap<>();
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
                    MetaWordNode bestNode = null;
                    for (IWordID id : wordSenses) {
                        IWord word = dict.getWord(id);
                        MetaWordNode currNode = addNodeToChains(instance, word, sentNum);

                        if (nodeExists(currNode)) {
                            bestNode = getStrongerNode(bestNode, currNode);
                        } else {    // word sense does not belong in existing chains, create new chain
                            currNode = new MetaWordNode(sentNum, word);
                            createNewChain(currNode);
                        }
                        instance.addMetaInstance(currNode);
                    }

                    instance.setBestNode(bestNode);
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
                newNode.chainNum = i;

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

    public String nonZeroChains() {
        StringBuilder sb = new StringBuilder();
        int numChains = 0;

        for (Metachain c: metachains) {
            if (c.strengthScore != 1.0) {
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
        SilberMcCoy test = new SilberMcCoy("nyc_rat_handtagged.txt");
        System.out.println();
        System.out.println("Nouns: " + test.nouns.size());
    }
}
