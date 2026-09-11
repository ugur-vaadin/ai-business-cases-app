package com.vaadin.demo.nordicsupply.ai;

import java.util.regex.Pattern;

/**
 * The parts a widget reply is read for: the title and the summary the model is asked to put on their own lines, and
 * whatever else it wrote. A part the model left out comes back as an empty string.
 */
public record WidgetReply(String title, String summary, String rest) {

    private static final Pattern TITLE = Pattern.compile("(?im)^\\s*\\**\\s*TITLE\\s*:\\**\\s*(.+?)\\s*$");
    private static final Pattern SUMMARY = Pattern.compile("(?im)^\\s*\\**\\s*SUMMARY\\s*:\\**\\s*(.+?)\\s*$");

    /** Reads the two lines out of a reply; the rest is the reply with those lines taken out. */
    public static WidgetReply parse(String response) {
        var title = TITLE.matcher(response);
        var summary = SUMMARY.matcher(response);
        return new WidgetReply(
                title.find() ? title.group(1).trim() : "",
                summary.find() ? summary.group(1).trim() : "",
                response.replaceAll("(?im)^\\s*\\**\\s*(TITLE|SUMMARY)\\s*:.*$", "")
                        .strip());
    }
}
