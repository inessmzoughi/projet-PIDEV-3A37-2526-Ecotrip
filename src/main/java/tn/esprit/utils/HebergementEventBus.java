package tn.esprit.utils;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;

public class HebergementEventBus {

    private static final List<Runnable> listeners = new ArrayList<>();

    // S'abonner
    public static void subscribe(Runnable listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // Se désabonner (important pour éviter les fuites mémoire)
    public static void unsubscribe(Runnable listener) {
        listeners.remove(listener);
    }

    // Déclencher — notifie tous les abonnés
    public static void publish() {
        Platform.runLater(() ->
                listeners.forEach(Runnable::run)
        );
    }
}