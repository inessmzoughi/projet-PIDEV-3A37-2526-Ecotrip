package tn.esprit.services.activity;

import tn.esprit.session.SessionManager;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ActivityFavoriteService {

    private static final String KEY_PREFIX = "activity-favorites-user-";
    private static final Preferences PREFS = Preferences.userRoot().node("tn/esprit/ecotrip/activity");

    public boolean isFavorite(int activityId) {
        return getFavoriteIds().contains(activityId);
    }

    public boolean toggleFavorite(int activityId) {
        Set<Integer> favorites = getFavoriteIds();
        boolean nowFavorite;
        if (favorites.contains(activityId)) {
            favorites.remove(activityId);
            nowFavorite = false;
        } else {
            favorites.add(activityId);
            nowFavorite = true;
        }
        saveFavorites(favorites);
        return nowFavorite;
    }

    public Set<Integer> getFavoriteIds() {
        String raw = PREFS.get(resolveKey(), "");
        if (raw.isBlank()) {
            return new TreeSet<>();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void saveFavorites(Set<Integer> favorites) {
        String raw = favorites.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        PREFS.put(resolveKey(), raw);
    }

    private String resolveKey() {
        int userId = SessionManager.getInstance().isLoggedIn()
                ? SessionManager.getInstance().getCurrentUser().getId()
                : 0;
        return KEY_PREFIX + userId;
    }
}
