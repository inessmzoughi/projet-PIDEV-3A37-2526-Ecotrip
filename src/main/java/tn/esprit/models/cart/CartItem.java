package tn.esprit.models.cart;

import tn.esprit.models.enums.ReservationType;

import java.time.LocalDate;
import java.util.Map;

public class CartItem {

    private ReservationType type;
    private int             itemId;       // hebergement/activity/transport id
    private String          label;        // display name in cart
    private double          unitPrice;    // price per night / per person / etc.
    private double          totalPrice;
    private LocalDate       dateFrom;
    private LocalDate       dateTo;
    private int             numberOfPersons;
    private Map<String, Object> details; // flexible: nights, guests, chambreId, etc.

    public CartItem() {}

    // ── Getters ─────────────────────────────────────
    public ReservationType getType()           { return type; }
    public int getItemId()                     { return itemId; }
    public String getLabel()                   { return label; }
    public double getUnitPrice()               { return unitPrice; }
    public double getTotalPrice()              { return totalPrice; }
    public LocalDate getDateFrom()             { return dateFrom; }
    public LocalDate getDateTo()               { return dateTo; }
    public int getNumberOfPersons()            { return numberOfPersons; }
    public Map<String, Object> getDetails()    { return details; }

    // ── Setters ─────────────────────────────────────
    public void setType(ReservationType type)          { this.type = type; }
    public void setItemId(int itemId)                  { this.itemId = itemId; }
    public void setLabel(String label)                 { this.label = label; }
    public void setUnitPrice(double unitPrice)         { this.unitPrice = unitPrice; }
    public void setTotalPrice(double totalPrice)       { this.totalPrice = totalPrice; }
    public void setDateFrom(LocalDate dateFrom)        { this.dateFrom = dateFrom; }
    public void setDateTo(LocalDate dateTo)            { this.dateTo = dateTo; }
    public void setNumberOfPersons(int n)              { this.numberOfPersons = n; }
    public void setDetails(Map<String, Object> d)      { this.details = d; }

    // ── Display label for cart UI ────────────────────
    public String getDisplayLabel() {
        return type.name() + " — " + label;
    }

    // Add to CartItem.java
    public long getNights() {
        if (dateFrom == null || dateTo == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo);
    }
}