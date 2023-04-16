import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.util.List;

// For usage in the Silber and McCoy algorithm
public class WordNode {
    private int sentNum;
    private IWord word;

    public WordNode(int sentNum, IWord word) {
        this.sentNum = sentNum;
        this.word = word;
    }

    public boolean isSynonym(IWord word) {
        return word.getSynset().getID() == this.word.getSynset().getID();
    }

    public boolean isHypernym(IDictionary dict, IWord word) {
        ISynset thisSet = this.word.getSynset();
        List<ISynsetID> hypernyms = thisSet.getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID sid: hypernyms) {
            for (IWord w: dict.getSynset(sid).getWords()) {
                if (w.equals(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateSentNum(int s) {
        sentNum = s;
    }

    public int getSentNum() {
        return sentNum;
    }

    public String toString() {
        return word.getLemma() + "-" + word.getSynset().getID();
    }

    public int computeDist(WordNode node) {
        return node.getSentNum() - this.sentNum;
    }
}
