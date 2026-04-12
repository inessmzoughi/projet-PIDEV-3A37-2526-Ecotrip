package tn.esprit.utils;

import tn.esprit.models.produit.Product;

import java.util.LinkedHashMap;
import java.util.Map;

public class CartManager {

    private static CartManager instance;
    private final Map<Product, Integer> items = new LinkedHashMap<>();

    private CartManager() {}

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    public void addProduct(Product p) {
        items.merge(p, 1, Integer::sum);
    }

    public void removeProduct(Product p) {
        items.remove(p);
    }

    public void updateQuantity(Product p, int qty) {
        if (qty <= 0) items.remove(p);
        else items.put(p, qty);
    }

    public Map<Product, Integer> getItems() { return items; }

    public double getTotal() {
        return items.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrix() * e.getValue())
                .sum();
    }

    public int getCount() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void clear() { items.clear(); }
}