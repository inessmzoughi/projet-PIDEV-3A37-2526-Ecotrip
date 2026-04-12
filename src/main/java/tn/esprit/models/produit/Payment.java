package tn.esprit.models.produit;

import java.util.Date;

public class Payment {
    private int id;
    private double amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Date paidAt;
    private int transactionId;
    private Date createdAt;
    private int reservationId;

    public Payment(int id, double amount, PaymentMethod paymentMethod, PaymentStatus paymentStatus, Date paidAt, int transactionId, Date createdAt, int reservationId) {
        this.id = id;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.reservationId = reservationId;
    }

    public Payment(double amount, PaymentMethod paymentMethod, PaymentStatus paymentStatus, Date paidAt, int transactionId, Date createdAt, int reservationId) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.reservationId = reservationId;
    }

    public Payment() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }
}
