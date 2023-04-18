import edu.mit.jwi.item.*;

import java.util.ArrayList;

// For usage in the Silber and McCoy algorithm
public class Metachain {
    public ArrayList<MetaWordNode> chain;
    public ISynsetID headSense;
    public double strengthScore;
    private int size;

    public Metachain(MetaWordNode wn) {
        headSense = wn.word.getSynset().getID();
        size = 1;
        strengthScore = 1;
        chain = new ArrayList<>();
        chain.add(wn);
    }

    public double getScore() {
        return strengthScore;
    }

    public int getSize() {
        return size;
    }

    public void insertWord(MetaWordNode chainWord , MetaWordNode word, double score) {
        chain.add(word);
        size++;
        strengthScore += score;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(strengthScore + ": {");
        for (MetaWordNode wn: chain) {
            sb.append(wn.toString()+",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
