package com.vaadin.demo.nordicsupply.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimNumbersTest {

    @Test
    void countsOnFromTheNewestNumber() {
        assertThat(ClaimNumbers.nextAfter("CL-2026-00123")).isEqualTo("CL-2026-00124");
    }

    @Test
    void keepsThePrefixAndTheDigitWidth() {
        assertThat(ClaimNumbers.nextAfter("A-0009")).isEqualTo("A-0010");
        assertThat(ClaimNumbers.nextAfter("CL-2026-00999")).isEqualTo("CL-2026-01000");
    }

    @Test
    void aNumberWithoutDigitsGetsOne() {
        assertThat(ClaimNumbers.nextAfter("X")).isEqualTo("X-1");
    }
}
