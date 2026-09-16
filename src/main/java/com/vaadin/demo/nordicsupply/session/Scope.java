package com.vaadin.demo.nordicsupply.session;

import java.util.List;

/**
 * A set of countries a user may see, with a label for the screen. A scope is either the boundary of a business role
 * (head office, or one of the regional managers) or a narrower choice the user makes in the country filter.
 */
public record Scope(String key, List<String> countries, String label) {

    /** A scope over the given countries, in the order they are listed. */
    public static Scope of(String key, String label, String... countries) {
        return new Scope(key, List.of(countries), label);
    }
}
