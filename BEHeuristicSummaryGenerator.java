import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
public class BEHeuristicSummaryGenerator {
    private final List<String> textSentences;
    private boolean[] checked;
    public BEHeuristicSummaryGenerator(String fileName) {
        textSentences = new ArrayList<>();
        Scanner sc = openScanner(fileName);
        buildSentenceArray(sc);

        checked = new boolean[textSentences.size()];
        System.out.println(textSentences.size());

        SilberMcCoy chainsAlgo = new SilberMcCoy(fileName);
        List<Metachain> strongChains = chainsAlgo.getStrongChains();

        for (Metachain c: strongChains) {
            int nodeIndex = 0;

            while (true) {
                if (nodeIndex < c.getSize()) {
                    int sentIndex = c.chain.get(nodeIndex).getSentNum() - 1;
                    if (sentIndex > textSentences.size()) {
                        nodeIndex++;
                    } else {
                        if (checked[sentIndex]) {
                            nodeIndex++;
                        } else {
                            System.out.println(textSentences.get(sentIndex));
                            checked[sentIndex] = true;
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }

        // Given a file, create metachains with a Silber and McCoy construction
        // ask for the most significant chains back
        // split file by text
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

    private void buildSentenceArray(Scanner sc) {
        // for each word, check tag. Stringbuild until you get to punctuation
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) {
            String[] split = sc.next().split("_");
            if (split[split.length - 1].equals(".")) {
                for (int i = 0; i < split.length - 1; i++) {
                    sb.append(split[i]);
                    sb.append(" ");
                }
                sb.deleteCharAt(sb.length() - 1);   // delete extra space
                sb.append(split[split.length - 1]);
                // System.out.println("FOUND: " + sb.toString());
                textSentences.add(sb.toString());
                sb.delete(0, sb.length());
            } else {
                for (int i = 0; i < split.length - 1; i++) {
                    sb.append(split[i]);
                    sb.append(" ");
                }
            }
        }
    }

    public static void main(String[] args) {
        BEHeuristicSummaryGenerator summaryGenerator = new BEHeuristicSummaryGenerator("NYT_articles/india_tagged.txt");

    }
}
