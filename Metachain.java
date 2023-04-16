import edu.mit.jwi.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// For usage in the Silber and McCoy algorithm
public class Metachain {
    private ArrayList<WordNode> chain;
    private double strengthScore;

    public Metachain() {
        strengthScore = 0;
        chain = new ArrayList<>();
    }

    public Metachain(WordNode wn) {
        new Metachain();
        chain.add(wn);
    }

    public double getScore() {
        return strengthScore;
    }

    public void insertWord(WordNode chainWord, WordNode word) {
        chain.add(word);
        strengthScore += chainWord.computeDist(word);
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
