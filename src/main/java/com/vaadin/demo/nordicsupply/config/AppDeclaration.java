package com.vaadin.demo.nordicsupply.config;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * The pack's declaration, {@code sql/app.json}: what the application needs to know about the dataset beyond its data.
 * The forms and the bulk change are written for Nordic Supply, so only the pack's identity and the dashboard's
 * suggestion chips come from here. Read once at start-up.
 */
public record AppDeclaration(String pack, String company, String title, List<String> dashboardChips) {

    // ---- parsing (the JSON is small and stable; hand-mapped to keep the records free of annotations)

    public static AppDeclaration from(JsonNode n) {
        return new AppDeclaration(
                text(n, "pack"), text(n, "company"), text(n, "title"), strings(n.get("dashboard_chips")));
    }

    private static List<String> strings(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n != null && n.isArray()) {
            n.forEach(v -> out.add(v.asString()));
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asString() : "";
    }
}
