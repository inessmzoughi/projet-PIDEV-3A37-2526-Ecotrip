package tn.esprit.services.activity;

import tn.esprit.models.activity.Activity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ActivityAssistantService {

    public ActivitySuggestionResponse suggestActivities(String userPrompt, List<Activity> activities) {
        String normalizedPrompt = normalize(userPrompt);
        List<Activity> safeActivities = activities == null ? List.of() : activities;

        List<ActivityMatch> ranked = safeActivities.stream()
                .filter(Activity::isActive)
                .map(activity -> new ActivityMatch(activity, scoreActivity(activity, normalizedPrompt)))
                .filter(match -> match.score > 0 || normalizedPrompt.isBlank())
                .sorted(Comparator.comparingInt(ActivityMatch::score).reversed()
                        .thenComparing(match -> match.activity().getPrice()))
                .limit(3)
                .collect(Collectors.toList());

        if (ranked.isEmpty() && !safeActivities.isEmpty()) {
            ranked = safeActivities.stream()
                    .filter(Activity::isActive)
                    .sorted(Comparator.comparingDouble(Activity::getPrice))
                    .limit(3)
                    .map(activity -> new ActivityMatch(activity, 1))
                    .collect(Collectors.toList());
        }

        List<Activity> suggestions = ranked.stream()
                .map(ActivityMatch::activity)
                .collect(Collectors.toCollection(ArrayList::new));

        String fallback = buildFallbackResponse(userPrompt, suggestions);
        String responseText = fallback;

        if (!suggestions.isEmpty()) {
            try {
                responseText = AiDescriptionService.generateActivitySuggestionReply(userPrompt, suggestions, fallback);
            } catch (Exception ignored) {
                responseText = fallback;
            }
        }

        return new ActivitySuggestionResponse(responseText, suggestions);
    }

    private int scoreActivity(Activity activity, String prompt) {
        if (prompt.isBlank()) {
            return 1;
        }

        int score = 0;
        score += contains(activity.getTitle(), prompt) ? 8 : 0;
        score += contains(activity.getLocation(), prompt) ? 7 : 0;
        score += contains(activity.getDescription(), prompt) ? 5 : 0;
        score += contains(activity.getCategory() != null ? activity.getCategory().getName() : null, prompt) ? 8 : 0;
        score += contains(activity.getGuide() != null ? activity.getGuide().toString() : null, prompt) ? 3 : 0;

        for (String token : prompt.split("\\s+")) {
            if (token.length() < 3) {
                continue;
            }
            score += contains(activity.getTitle(), token) ? 3 : 0;
            score += contains(activity.getLocation(), token) ? 3 : 0;
            score += contains(activity.getDescription(), token) ? 2 : 0;
            score += contains(activity.getCategory() != null ? activity.getCategory().getName() : null, token) ? 3 : 0;
        }

        return score;
    }

    private boolean contains(String source, String search) {
        return source != null && !search.isBlank() && normalize(source).contains(search);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String buildFallbackResponse(String userPrompt, List<Activity> suggestions) {
        if (suggestions.isEmpty()) {
            return "Je n'ai pas trouve d'activite parfaitement alignee avec cette demande pour le moment. Essayez avec une categorie, une ville, une ambiance ou un budget pour que je puisse affiner la suggestion.";
        }

        Activity first = suggestions.get(0);
        StringBuilder builder = new StringBuilder();
        builder.append("Pour ");
        builder.append(userPrompt == null || userPrompt.isBlank() ? "une envie d'evasion" : "\"" + userPrompt.trim() + "\"");
        builder.append(", je vous recommande d'abord ");
        builder.append(first.getTitle());
        builder.append(" a ");
        builder.append(first.getLocation());
        builder.append(", une experience ");
        builder.append(first.getCategory() != null ? first.getCategory().getName().toLowerCase(Locale.ROOT) : "nature");
        builder.append(" pensee pour un tourisme plus doux et immersif.");

        if (suggestions.size() > 1) {
            builder.append(" Vous pouvez aussi regarder ");
            builder.append(suggestions.stream()
                    .skip(1)
                    .map(Activity::getTitle)
                    .collect(Collectors.joining(" et ")));
            builder.append(" pour comparer le rythme, le lieu et l'ambiance.");
        }

        return builder.toString();
    }

    private record ActivityMatch(Activity activity, int score) {
    }

    public record ActivitySuggestionResponse(String responseText, List<Activity> suggestions) {
    }
}
