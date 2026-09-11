package com.vaadin.demo.nordicsupply.util;

/**
 * What to show when something underneath fails. A JDBC or provider failure arrives wrapped in several layers; the
 * innermost message is the one that says anything, and it is clipped so a notification stays readable.
 */
public final class Errors {

    private Errors() {}

    /** The root cause's message, cut to {@code maxLength} characters with an ellipsis when it was cut. */
    public static String rootMessage(Throwable t, int maxLength) {
        var cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        var message = String.valueOf(cause.getMessage());
        return message.length() > maxLength ? message.substring(0, maxLength) + "…" : message;
    }
}
