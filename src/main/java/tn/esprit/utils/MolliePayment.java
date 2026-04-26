package tn.esprit.utils;


public class MolliePayment {
    private final String id;
    private final String checkoutUrl;
    private final String status;

    public MolliePayment(String id, String checkoutUrl, String status) {
        this.id = id;
        this.checkoutUrl = checkoutUrl;
        this.status = status;
    }

    public String getId()          { return id; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getStatus()      { return status; }
}