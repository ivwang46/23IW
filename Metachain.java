import edu.mit.jwi.item.*;

import java.util.ArrayList;

// For usage in the Silber and McCoy algorithm
public class Metachain {
    public ArrayList<MetaWordNode> chain;
    public ISynsetID headSense;
    public double strengthScore;

    public Metachain(MetaWordNode wn) {
        headSense = wn.word.getSynset().getID();
        strengthScore = 1;
        chain = new ArrayList<>();
        chain.add(wn);
    }

    public double getScore() {
        return strengthScore;
    }

    public int getSize() {
        return chain.size();
    }

    public void insertWord(MetaWordNode word, double score) {
        chain.add(word);
        strengthScore += score;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(strengthScore + ": {");
        for (MetaWordNode wn: chain) {
            // if (wn.score != 0.0) sb.append(wn+",");
            sb.append(wn+",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
