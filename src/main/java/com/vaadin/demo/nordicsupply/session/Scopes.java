package com.vaadin.demo.nordicsupply.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every scope the application knows: the four business roles and one scope per single country. The countries are
 * the values of the {@code country} column of the customers, so a scope can be compared with the data directly.
 * Scopes are fixed in code; nothing a user types ever becomes one.
 */
public final class Scopes {

    public static final Scope ALL = Scope.of("all", "All countries", "FI", "EE", "SE", "NO", "DK", "DE");
    public static final Scope FI_EE = Scope.of("fi_ee", "Finland and Estonia", "FI", "EE");
    public static final Scope SE_NO = Scope.of("se_no", "Sweden and Norway", "SE", "NO");
    public static final Scope DK_DE = Scope.of("dk_de", "Denmark and Germany", "DK", "DE");
    public static final Scope FI = Scope.of("fi", "Finland", "FI");
    public static final Scope EE = Scope.of("ee", "Estonia", "EE");
    public static final Scope SE = Scope.of("se", "Sweden", "SE");
    public static final Scope NO = Scope.of("no", "Norway", "NO");
    public static final Scope DK = Scope.of("dk", "Denmark", "DK");
    public static final Scope DE = Scope.of("de", "Germany", "DE");

    /** The scopes a business role can have; the others are only filter choices. */
    private static final List<Scope> ROLE_SCOPES = List.of(ALL, FI_EE, SE_NO, DK_DE);

    private static final List<Scope> ALL_SCOPES = List.of(ALL, FI_EE, SE_NO, DK_DE, FI, EE, SE, NO, DK, DE);

    private Scopes() {}

    public static List<Scope> all() {
        return ALL_SCOPES;
    }

    public static Optional<Scope> byKey(String key) {
        return ALL_SCOPES.stream().filter(s -> s.key().equals(key)).findFirst();
    }

    /** The scope of a business role, or empty when the key is not one of the four. */
    public static Optional<Scope> roleScope(String key) {
        return ROLE_SCOPES.stream().filter(s -> s.key().equals(key)).findFirst();
    }

    /**
     * What the country filter may offer inside a scope: the scope itself, then each of its countries alone. A
     * single-country scope is keyed by its country code in lower case, which is what the lookup relies on.
     */
    public static List<Scope> filtersWithin(Scope scope) {
        var out = new ArrayList<Scope>();
        out.add(scope);
        for (var country : scope.countries()) {
            byKey(country.toLowerCase()).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /** True when everything the narrower scope covers is inside the wider one. */
    public static boolean isWithin(Scope narrower, Scope wider) {
        return wider.countries().containsAll(narrower.countries());
    }

    /** How a user is offered in the switcher: the name, then the region they are responsible for. */
    public static String demoLabel(String fullName, Scope roleScope) {
        return fullName + " · " + (roleScope.equals(ALL) ? "Head office" : roleScope.label());
    }
}
