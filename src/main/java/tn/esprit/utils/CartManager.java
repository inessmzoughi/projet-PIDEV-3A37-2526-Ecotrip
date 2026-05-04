package tn.esprit.utils;

import javafx.application.Platform;
import tn.esprit.models.cart.CartItem;
import tn.esprit.models.produit.Product;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CartManager {

    // ── Durées ────────────────────────────────────────────────────────────────
    public static final long EXPIRY_MILLIS  = 5L * 60 * 1000;   // 5 min
    public static final long WARNING_MILLIS = 60_000L;           // warning 1 min avant

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static CartManager instance;
    private CartManager() {}
    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    // ── Données du panier ─────────────────────────────────────────────────────
    private final Map<Product, Integer> productItems     = new LinkedHashMap<>();
    private final List<CartItem>        reservationItems = new ArrayList<>();

    // ── Horodatage d'ajout ────────────────────────────────────────────────────
    private final Map<Product,  Long> productAddTime     = new LinkedHashMap<>();
    private final Map<CartItem, Long> reservationAddTime = new IdentityHashMap<>();

    // ── Scheduler ─────────────────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cart-expiry-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final Map<Object, ScheduledFuture<?>> warningFutures = new IdentityHashMap<>();
    private final Map<Object, ScheduledFuture<?>> expiryFutures  = new IdentityHashMap<>();

    // ── Callbacks UI ─────────────────────────────────────────────────────────
    /** Appelé sur le FX Thread quand un article est à 1 min d'expiration. Reçoit le label de l'article. */
    private Consumer<String> onWarning;
    /** Appelé sur le FX Thread quand un article a expiré et a été retiré. Reçoit le label de l'article. */
    private Consumer<String> onExpired;
    /** Appelé sur le FX Thread à chaque tick (1 s) pour rafraîchir les timers visuels. */
    private Runnable onTick;

    public void setOnWarning(Consumer<String> cb) { this.onWarning = cb; }
    public void setOnExpired(Consumer<String> cb) { this.onExpired = cb; }
    public void setOnTick(Runnable cb)            { this.onTick = cb;    }

    // ── Ticker global (rafraîchit les timers dans l'UI toutes les secondes) ───
    private ScheduledFuture<?> globalTicker;

    public void startTicker() {
        stopTicker();
        globalTicker = scheduler.scheduleAtFixedRate(
                () -> Platform.runLater(() -> { if (onTick != null) onTick.run(); }),
                1, 1, TimeUnit.SECONDS);
    }

    public void stopTicker() {
        if (globalTicker != null) { globalTicker.cancel(false); globalTicker = null; }
    }

    // ══ Products ══════════════════════════════════════════════════════════════

    public void addProduct(Product p) {
        productItems.merge(p, 1, Integer::sum);
        productAddTime.put(p, System.currentTimeMillis());
        scheduleExpiry(p, p.getNom());
    }

    public void removeProduct(Product p) {
        productItems.remove(p);
        productAddTime.remove(p);
        cancelFutures(p);
    }

    public void updateQuantity(Product p, int qty) {
        if (qty <= 0) removeProduct(p);
        else productItems.put(p, qty);
    }

    public Map<Product, Integer> getProductItems() { return productItems; }

    public double getProductTotal() {
        return productItems.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrix() * e.getValue()).sum();
    }

    public int getProductCount() {
        return productItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    // ══ Reservation items ═════════════════════════════════════════════════════

    public void addReservationItem(CartItem item) {
        reservationItems.add(item);
        reservationAddTime.put(item, System.currentTimeMillis());
        scheduleExpiry(item, item.getLabel());
    }

    public void removeReservationItem(CartItem item) {
        reservationItems.remove(item);
        reservationAddTime.remove(item);
        cancelFutures(item);
    }

    public List<CartItem> getReservationItems()  { return reservationItems; }

    public double getReservationTotal() {
        return reservationItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public int getReservationCount() { return reservationItems.size(); }

    // ══ Grand total ═══════════════════════════════════════════════════════════

    public double getTotal() { return getProductTotal() + getReservationTotal(); }
    public int    getCount() { return getProductCount()  + getReservationCount(); }

    // ══ Clear ═════════════════════════════════════════════════════════════════

    public void clear() {
        new ArrayList<>(productItems.keySet()).forEach(this::cancelFutures);
        new ArrayList<>(reservationItems).forEach(this::cancelFutures);
        productItems.clear();
        reservationItems.clear();
        productAddTime.clear();
        reservationAddTime.clear();
        stopTicker();
    }

    public void clearProducts() {
        new ArrayList<>(productItems.keySet()).forEach(this::cancelFutures);
        productItems.clear();
        productAddTime.clear();
    }

    public void clearReservations() {
        new ArrayList<>(reservationItems).forEach(this::cancelFutures);
        reservationItems.clear();
        reservationAddTime.clear();
    }

    // ══ Temps restant ═════════════════════════════════════════════════════════

    public long getRemainingMillis(Product p) {
        Long added = productAddTime.get(p);
        if (added == null) return -1;
        return Math.max(0, EXPIRY_MILLIS - (System.currentTimeMillis() - added));
    }

    public long getRemainingMillis(CartItem item) {
        Long added = reservationAddTime.get(item);
        if (added == null) return -1;
        return Math.max(0, EXPIRY_MILLIS - (System.currentTimeMillis() - added));
    }

    /**
     * Retourne true si l'article est dans la phase "warning" (< WARNING_MILLIS restant).
     */
    public boolean isWarning(Product p) {
        return getRemainingMillis(p) <= WARNING_MILLIS;
    }

    public boolean isWarning(CartItem item) {
        return getRemainingMillis(item) <= WARNING_MILLIS;
    }

    /** Formate un délai en millisecondes en chaîne lisible : "4min 32s" */
    public static String formatRemaining(long millis) {
        if (millis <= 0) return "expiré";
        long s   = millis / 1000;
        long h   = s / 3600;
        long min = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0)   return String.format("%dh %02dmin %02ds", h, min, sec);
        if (min > 0) return String.format("%dmin %02ds", min, sec);
        return String.format("%ds", sec);
    }

    // ══ Timer interne ═════════════════════════════════════════════════════════

    private void scheduleExpiry(Object key, String label) {
        cancelFutures(key);
        long warnDelay = EXPIRY_MILLIS - WARNING_MILLIS;

        // Warning : 1 min avant expiration
        ScheduledFuture<?> wf = scheduler.schedule(
                () -> Platform.runLater(() -> {
                    if (onWarning != null) onWarning.accept(label);
                }),
                warnDelay, TimeUnit.MILLISECONDS);

        // Expiration : retrait automatique
        ScheduledFuture<?> ef = scheduler.schedule(
                () -> Platform.runLater(() -> {
                    boolean removed = false;
                    if (key instanceof Product p && productItems.containsKey(p)) {
                        removeProduct(p);
                        removed = true;
                    } else if (key instanceof CartItem ci && reservationItems.contains(ci)) {
                        removeReservationItem(ci);
                        removed = true;
                    }
                    if (removed && onExpired != null) onExpired.accept(label);
                }),
                EXPIRY_MILLIS, TimeUnit.MILLISECONDS);

        warningFutures.put(key, wf);
        expiryFutures.put(key, ef);
    }

    private void cancelFutures(Object key) {
        ScheduledFuture<?> wf = warningFutures.remove(key);
        if (wf != null) wf.cancel(false);
        ScheduledFuture<?> ef = expiryFutures.remove(key);
        if (ef != null) ef.cancel(false);
    }

    public void shutdown() { scheduler.shutdownNow(); }
}