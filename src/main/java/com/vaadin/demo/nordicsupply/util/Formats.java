package com.vaadin.demo.nordicsupply.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import com.vaadin.flow.component.UI;

/**
 * How counts, money and raw database values are written on screen. One place for all of them, following the
 * browser's own locale, so a Finnish reader sees "9 034,50" where an English one sees "9,034.50".
 */
public final class Formats {

    private Formats() {}

    /** A count: grouped, no decimals. */
    public static String number(long n) {
        return NumberFormat.getIntegerInstance(locale()).format(n);
    }

    /** An amount: grouped, always two decimals. The currency is named by the column, not repeated per row. */
    public static String money(BigDecimal amount) {
        var money = NumberFormat.getNumberInstance(locale());
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        return money.format(amount);
    }

    /** A value straight out of a result row, as the catalogue shows it. */
    public static String value(Object v) {
        return switch (v) {
            case null -> "";
            case BigDecimal b -> money(b);
            case LocalDateTime d -> d.withNano(0).toString().replace('T', ' ');
            case LocalDate d -> d.toString();
            case Boolean b -> b ? "Yes" : "No";
            default -> String.valueOf(v);
        };
    }

    private static Locale locale() {
        return UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.getDefault();
    }
}
