package com.vaadin.demo.nordicsupply.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppDeclarationTest {

    private static final String PACK = "nordic_supply";

    @Test
    void readsTheDeclarationOfTheDefaultPack() {
        var declaration = new PackData(PACK).declaration();
        assertThat(declaration.pack()).isEqualTo("nordic_supply");
        assertThat(declaration.company()).isEqualTo("Nordic Supply");
        assertThat(declaration.dashboardChips()).hasSize(5);
    }

    @Test
    void theTitleIsThereForTheHeading() {
        assertThat(new PackData(PACK).declaration().title()).isNotBlank();
    }
}
