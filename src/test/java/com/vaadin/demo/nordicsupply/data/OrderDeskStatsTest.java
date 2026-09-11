package com.vaadin.demo.nordicsupply.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The five numbers on the landing page, for the dataset's as-of date. */
@SpringBootTest
@ActiveProfiles("test")
class OrderDeskStatsTest {

    @Autowired
    private OrderDeskStats stats;

    @Test
    void countsWhatTheManualCaseSays() {
        var tiles = stats.tiles(LocalDate.of(2026, 9, 7));
        assertThat(tiles).hasSize(5);
        assertThat(tiles.stream().map(OrderDeskStats.Tile::value)).containsExactly(55, 67, 361, 3060, 2283);
        assertThat(tiles.stream().map(OrderDeskStats.Tile::label))
                .containsExactly(
                        "Orders this month",
                        "Open claims",
                        "Late dispatches last month",
                        "Active products",
                        "Customers");
    }
}
