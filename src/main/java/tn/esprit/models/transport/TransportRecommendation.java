package tn.esprit.models.transport;

public class TransportRecommendation {
    private int rank;
    private String type;
    private String price;
    private String justification;

    public TransportRecommendation() {
    }

    public TransportRecommendation(int rank, String type, String price, String justification) {
        this.rank = rank;
        this.type = type;
        this.price = price;
        this.justification = justification;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
