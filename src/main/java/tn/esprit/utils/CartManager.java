package tn.esprit.utils;

import tn.esprit.models.cart.CartItem;
import tn.esprit.models.produit.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CartManager {

    private static CartManager instance;

    // Products (boutique)
    private final Map<Product, Integer> productItems = new LinkedHashMap<>();

    // Reservation items (hebergement, activity, transport)
    private final List<CartItem> reservationItems = new ArrayList<>();

    private CartManager() {}

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    // ── Products (existing logic — unchanged) ────────
    public void addProduct(Product p) {
        productItems.merge(p, 1, Integer::sum);
    }
    public void removeProduct(Product p)            { productItems.remove(p); }
    public void updateQuantity(Product p, int qty)  {
        if (qty <= 0) productItems.remove(p); else productItems.put(p, qty);
    }
    public Map<Product, Integer> getProductItems()  { return productItems; }

    public double getProductTotal() {
        return productItems.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrix() * e.getValue()).sum();
    }
    public int getProductCount() {
        return productItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    // ── Reservation items (new) ──────────────────────
    public void addReservationItem(CartItem item)      { reservationItems.add(item); }
    public void removeReservationItem(CartItem item)   { reservationItems.remove(item); }
    public List<CartItem> getReservationItems()        { return reservationItems; }

    public double getReservationTotal() {
        return reservationItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }
    public int getReservationCount()                   { return reservationItems.size(); }

    // ── Grand total (all items) ──────────────────────
    public double getTotal()  { return getProductTotal() + getReservationTotal(); }
    public int    getCount()  { return getProductCount() + getReservationCount(); }

    // ── Clear ────────────────────────────────────────
    public void clear() {
        productItems.clear();
        reservationItems.clear();
    }
    public void clearProducts()     { productItems.clear(); }
    public void clearReservations() { reservationItems.clear(); }
}