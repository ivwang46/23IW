import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// If a Word Instance contains a list of all its MetaNodes, it should contain a pointer to the bestNode
// for every Node that is not that Node, set its score to zero and subtract from the total score of the chain
public class WordInstance {
    public final IIndexWord idxWord;
    private final int sentNum;
    private List<MetaWordNode> metaInstances;
    private MetaWordNode bestContribution;

    public WordInstance(IIndexWord idxWord, int sentNum) {
        this.sentNum = sentNum;
        this.idxWord = idxWord;
        metaInstances = new ArrayList<>();
    }

    public void addMetaInstance(MetaWordNode n) {
        metaInstances.add(n);
    }

    public void setBestNode(MetaWordNode best) {
        bestContribution = best;
    }

    public List<MetaWordNode> getMetaInstances() {
        return metaInstances;
    }

    public MetaWordNode getBestContribution() {
        return bestContribution;
    }

    public int getSentNum() {
        return sentNum;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Word Instance: " + idxWord.getLemma() + "\n");
        sb.append("Found in sentence number: " + sentNum + "\n");
        sb.append("Initial metanodes: " + metaInstances.size());
        return sb.toString();
    }
}
