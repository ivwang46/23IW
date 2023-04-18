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
    public double score;
    public int chainNum;
    private final int sentNum;


    public MetaWordNode(int sentNum, IWord word) {
        this.sentNum = sentNum;
        this.word = word;
    }

    public boolean isSynonym(ISynsetID headSense) {
//        System.out.print(headSense);
//        System.out.print(" " + this.word.getSynset().getID());
//        System.out.println();
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

    public int getSentNum() {
        return sentNum;
    }

    public String toString() {
        return word.getLemma() + "-" + word.getSynset().getID();
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

//            List<IWordID> coffeeIDS = word1.getWordIDs();
//            List<IWordID> latteIDs = word2.getWordIDs();
//            for (IWordID c: coffeeIDS) {
//                for (IWordID l: latteIDs) {
//                    System.out.println(c.getLemma() + " / " + l.getLemma());
//                    IWord coffee = dict.getWord(c);
//                    IWord latte = dict.getWord(l);
//                    MetaWordNode cn = new MetaWordNode(0, coffee);
//                    MetaWordNode ln = new MetaWordNode(0, latte);
//                    System.out.println(coffee.getSynset().getID() + " / " + latte.getSynset().getID());
//                    System.out.println("SYNONYM: " + cn.isSynonym(latte));
//                    System.out.println("HYPERNYM: " + ((cn.isHypernym(dict, latte) || (ln.isHypernym(dict, coffee)))));
//                    System.out.println();
//                }
//            }
        } catch (IOException e) {
            System.out.println("ERR: " + e.getMessage());
        }
    }
}
