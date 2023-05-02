import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.Pointer;

import java.util.List;

public class LexicalAnalysis {
    public static boolean isNoun(String tag) {
        if (tag.length() >= 2) {
            return tag.substring(0, 2).equals("NN");
        }
        return false;
    }

    public static boolean isVerb(String tag) {
        if (tag.length() >= 2) {
            return tag.substring(0, 2).equals("VB");
        }
        return false;
    }

    public static boolean isSentenceEnd(String tag) {
        return tag.equals(".");
    }

    public static boolean isAdjective(String tag) {
        if (tag.length() >= 2) {
            return tag.substring(0, 2).equals("JJ");
        }
        return false;
    }

    public static boolean isSynonym(ISynsetID s1, ISynsetID s2) {
        return s1.equals(s2);
    }

    public static boolean isHypernym(IDictionary dict, ISynsetID chain, ISynsetID node) {
        List<ISynsetID> hypernyms = dict.getSynset(chain).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h: hypernyms) {
            if (h.equals(node)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHyponym(IDictionary dict, ISynsetID chain, ISynsetID node) {
        List<ISynsetID> hypernyms = dict.getSynset(chain).getRelatedSynsets(Pointer.HYPONYM);
        for (ISynsetID h: hypernyms) {
            if (h.equals(node)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSibling(IDictionary dict, ISynsetID s1, ISynsetID s2) {
        List<ISynsetID> hypernyms1 = dict.getSynset(s1).getRelatedSynsets(Pointer.HYPERNYM);
        List<ISynsetID> hypernyms2 = dict.getSynset(s2).getRelatedSynsets(Pointer.HYPERNYM);
        for (ISynsetID h1: hypernyms1) {
            for (ISynsetID h2: hypernyms2) {
                if (h1.equals(h2)) {
                    // System.out.println("FOUND SIBLINGS OF: " + dict.getSynset(h1).getGloss());
                    return true;
                }
            }
        }
        return false;
    }
}
