public class Sentence {
    public String text;
    public final int sentNum;
    private double uniqueWords = 0.0;
    private double unitSignificanceScore = 0.0;
    private double cohesionScore = 0.0;

    public Sentence(int sentNum, String text) {
        this.sentNum = sentNum;
        this.text = text;
    }

    public Sentence(double score, int sentNum, String text, double cohesionScore) {
        this.text = text;
        this.sentNum = sentNum;
        this.unitSignificanceScore = score;
        this.cohesionScore = cohesionScore;
    }

    public void setUniqueWords(double num) {
        this.uniqueWords = num;
    }

    public void setCohesionScore(double score) {
        this.cohesionScore = score;
    }


    public double getCohesionScore() {
        return cohesionScore;
    }

    public double getUniqueScore() {
        return uniqueWords;
    }

    public String toString() {
        return text;
    }
}
