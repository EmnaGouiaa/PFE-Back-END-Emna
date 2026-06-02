package fsegs.pfebackendemnagouuiaa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Regroupe les tâches Trello en colonnes À faire / En cours / Terminé pour le cahier de stage.
 */
public final class LogbookTrelloSupport {

    public static final String COLUMN_TODO = "A faire";
    public static final String COLUMN_IN_PROGRESS = "En cours";
    public static final String COLUMN_DONE = "Termine";

    private LogbookTrelloSupport() {
    }

    public static Map<String, List<Map<String, Object>>> groupTasksByColumn(
            List<Map<String, Object>> tasks,
            Map<String, String> listNames) {
        Map<String, List<Map<String, Object>>> columns = new LinkedHashMap<>();
        columns.put(COLUMN_TODO, new ArrayList<>());
        columns.put(COLUMN_IN_PROGRESS, new ArrayList<>());
        columns.put(COLUMN_DONE, new ArrayList<>());

        if (tasks == null) {
            return columns;
        }

        for (Map<String, Object> task : tasks) {
            if (task == null) {
                continue;
            }
            String listId = String.valueOf(task.getOrDefault("idList", ""));
            String listName = listNames != null ? listNames.getOrDefault(listId, "") : "";
            String column = resolveColumn(listName);
            columns.computeIfAbsent(column, key -> new ArrayList<>()).add(task);
        }
        return columns;
    }

    public static String resolveColumn(String listName) {
        String normalized = listName == null ? "" : listName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("done") || normalized.contains("termin") || normalized.contains("fini")
                || normalized.contains("complete")) {
            return COLUMN_DONE;
        }
        if (normalized.contains("doing") || normalized.contains("progress") || normalized.contains("cours")
                || normalized.contains("wip")) {
            return COLUMN_IN_PROGRESS;
        }
        return COLUMN_TODO;
    }
}
