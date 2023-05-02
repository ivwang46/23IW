import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class NewHeuristic {
    private List<Sentence> sentences;
    private Map<String, Integer> numOccurences;
    private IDictionary dict;
    private int totalWords;
    private double meanScore;
    private double stdevScore;
    public NewHeuristic(String filename) {
        Scanner sc = openScanner(filename);
        openDict();
        sentences = new ArrayList<>();
        numOccurences = new HashMap<>();
        scanSentences(sc);


        computeSentenceScores();
        for (Sentence s: sentences) {
            System.out.println(s.getCohesionScore()+"-"+s.text);
        }

        computeStatistics();

        SilberMcCoy smc = new SilberMcCoy(filename);
        List<Metachain> chains = smc.getStrongChains();
        printSummary(chains);
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

    private void scanSentences(Scanner sc) {
        int sentNum = 1;
        int numWords = 0;
        StringBuilder sb = new StringBuilder();
        List<String> uniqueWords = new ArrayList<>();

        while (sc.hasNext()) {
            String[] split = sc.next().split("_");
            if (LexicalAnalysis.isSentenceEnd(split[split.length - 1])) {
                sb.deleteCharAt(sb.length() - 1);
                sb.append(split[0]);
                Sentence s = new Sentence(sentNum, sb.toString());
                s.setUniqueWords((double)uniqueWords.size()/(double)numWords);   // UNIQUE WORDS PER NUM WORDS
                sentences.add(s);
                sentNum++;
                totalWords += numWords;

                // reset containers
                numWords = 0;
                sb = new StringBuilder("");
                uniqueWords = new ArrayList<>();
            } else {
                String tag = split[split.length - 1];
                numWords++;
                StringBuilder little_sb = new StringBuilder("");
                for (int i = 0; i < split.length - 1; i++) {
                    little_sb.append(split[i] + " ");
                }
                little_sb.deleteCharAt(little_sb.length() - 1);
                String curr = little_sb.toString();
                IIndexWord word = getWord(curr, tag);

                if (word != null) {
                    if (!uniqueWords.contains(word.getLemma())) {
                        uniqueWords.add(word.getLemma());
                    }
                    updateOccurences(word.getLemma());
                }
                sb.append(curr + " ");
            }
        }
    }

    private IIndexWord getWord(String curr, String tag) {
        IIndexWord word = null;
        if (LexicalAnalysis.isNoun(tag)) {
            word = dict.getIndexWord(curr, POS.NOUN);
        } else if (LexicalAnalysis.isVerb(tag)) {
            word = dict.getIndexWord(curr, POS.VERB);
        } else if (LexicalAnalysis.isAdjective(tag)) {
            word = dict.getIndexWord(curr, POS.ADJECTIVE);
        }
        return word;
    }

    private void updateOccurences(String word) {
        if (numOccurences.containsKey(word)) {
            numOccurences.put(word, numOccurences.get(word) + 1);
        } else {
            numOccurences.put(word, 1);
        }
    }

    private void computeSentenceScores() {
        for (Sentence s: sentences) {
            // System.out.println(s.text);
            double unique = s.getUniqueScore();
            String[] split = s.text.split(" ");
            double sum = 0.0;
            for (String word: split) {
                if (numOccurences.containsKey(word)) {
                    System.out.println((double) numOccurences.get(word)/totalWords*1000);
                    sum += Math.sin(0.25*((double) numOccurences.get(word)/totalWords*1000));
                }
            }
            double currScore;
            if (sum == 0.0) {
                currScore = 0.0;
                s.setCohesionScore(currScore);
            } else {
                currScore = (sum) + unique;
            }
            s.setCohesionScore(currScore);
            meanScore += (currScore);
        }
        meanScore = meanScore / sentences.size();
    }

    private void computeStatistics() {
        double sum = 0.0;
        for (Sentence s: sentences) {
            double score = s.getCohesionScore();
            sum += Math.pow(score - meanScore, 2);
        }
        stdevScore = Math.sqrt(sum/sentences.size());
        System.out.println("MEAN SCORE: " + meanScore);
        System.out.println("STDEV SCORE: " + stdevScore);
    }

    private void printSummary(List<Metachain> chains) {
        boolean[] visited = new boolean[sentences.size()];

        for (Metachain c: chains) {

            for (int i = 0; i < c.getSize(); i++) {
                int sentNum = c.chain.get(i).getSentNum() - 1;
                double currScore = sentences.get(sentNum).getCohesionScore();
                if (currScore - meanScore > stdevScore) {
                    System.out.print("*");
                    visited[sentNum] = true;
                }
            }
        }

        System.out.println();
        int numSentences = 0;
        System.out.println("Summary: ");
        for (int i = 0; i < visited.length; i++) {
            if (visited[i]) {
                numSentences++;
                System.out.println(sentences.get(i));
            }
        }
        System.out.println(numSentences + " sentences in summary.");
        System.out.println(sentences.size() + " sentences originally");
    }

    public static void main(String[] args) {
        NewHeuristic n = new NewHeuristic("NYT_articles/sargassum_tagged.txt");
    }
}
