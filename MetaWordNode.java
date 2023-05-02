import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

// For usage in the Silber and McCoy algorithm

public class MetaWordNode {
    public final IWord word;
    private final int sentNum;
    public double score;
    public int chainNum;

    public MetaWordNode(int sentNum, IWord word) {
        this.sentNum = sentNum;
        this.word = word;
    }

    public boolean isSynonym(ISynsetID headSense) {
        return headSense.equals(this.word.getSynset().getID());
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

    public boolean isHyperHypo(IDictionary dict, ISynsetID sense) {
        ISynset thisSet = this.word.getSynset();
        List<ISynsetID> hypernyms = thisSet.getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h: hypernyms) {
            if (h.equals(sense)) {
                return true;
            }
        }

        List<ISynsetID> hyponyms = thisSet.getRelatedSynsets(Pointer.HYPONYM);
        for (ISynsetID h: hyponyms) {
            if (h.equals(sense)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSibling(IDictionary dict, ISynsetID sense) {
        ISynset thisSet = this.word.getSynset();
        List<ISynsetID> hypernyms1 = thisSet.getRelatedSynsets(Pointer.HYPERNYM);
        List<ISynsetID> hypernyms2 = dict.getSynset(sense).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h1: hypernyms1) {
            for (ISynsetID h2: hypernyms2) {
                if (h1.equals(h2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getSentNum() {
        return sentNum;
    }

    public String toString() {
        return score+"-"+word.getLemma() + "-" + word.getSynset().getID();
    }

    public int computeDist(MetaWordNode laterNode) {
        // System.out.println(laterNode.getSentNum() + " " + this.sentNum);
        return laterNode.getSentNum() - this.sentNum;
    }

    public static void main(String[] args) {
        try {       // open WordNet dictionary
            Dictionary dict;
            String wnhome = "./WordNet-3.0";
            String path = wnhome + File.separator + "dict";
            URL url = new URL("file", null, path);

            dict = new Dictionary(url);
            dict.open();

            IIndexWord word1 = dict.getIndexWord("coffee", POS.NOUN);
            IIndexWord word2 = dict.getIndexWord("test", POS.NOUN);
            IIndexWord word3 = dict.getIndexWord("animal", POS.NOUN);

            IWordID wid1 = word1.getWordIDs().get(0);
            IWordID wid2 = word2.getWordIDs().get(0);
            System.out.println(wid1.getSynsetID().equals(wid2.getSynsetID()));
        } catch (IOException e) {
            System.out.println("ERR: " + e.getMessage());
        }
    }
}
