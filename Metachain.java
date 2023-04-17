import edu.mit.jwi.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// For usage in the Silber and McCoy algorithm
public class Metachain {
    public ArrayList<WordNode> chain;
    public ISynsetID headSense;
    private double strengthScore;
    private int size;

    public Metachain(WordNode wn) {
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

    public void insertWord(WordNode chainWord , WordNode word, double score) {
        chain.add(word);
        size++;
        strengthScore += score;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(strengthScore + ": {");
        for (WordNode wn: chain) {
            sb.append(wn.toString()+",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
