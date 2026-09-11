package com.vaadin.demo.nordicsupply.data;

import java.util.regex.Pattern;

/** How a claim number continues the series in the data: keep the prefix and the digit width, add one. */
public final class ClaimNumbers {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("^(.*?)(\\d+)$");

    private ClaimNumbers() {}

    /** The number that follows the given one, e.g. {@code CL-2026-00123} becomes {@code CL-2026-00124}. */
    public static String nextAfter(String last) {
        var m = TRAILING_DIGITS.matcher(last);
        if (!m.find()) {
            return last + "-1";
        }
        var digits = m.group(2);
        var next = Long.parseLong(digits) + 1;
        return m.group(1) + String.format("%0" + digits.length() + "d", next);
    }
}
