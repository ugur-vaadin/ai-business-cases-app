package com.vaadin.demo.nordicsupply;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.demo.nordicsupply.data.OrderDeskStats;
import com.vaadin.demo.nordicsupply.data.SavedWidgets;

/** The pack really loads into H2, and the classes that read it see the seeded data. */
@SpringBootTest
@ActiveProfiles("test")
class PackLoadTest {

    @Autowired
    private SavedWidgets widgets;

    @Autowired
    private OrderDeskStats stats;

    @Test
    void theSeededDashboardIsThere() {
        assertThat(widgets.forUser(1)).hasSize(2);
    }

    @Test
    void theHomeTilesCountWhatTheManualCaseSays() {
        var tiles = stats.tiles(LocalDate.of(2026, 9, 7));
        assertThat(tiles).hasSize(5);
        assertThat(tiles.stream().map(OrderDeskStats.Tile::value)).containsExactly(55, 67, 361, 3060, 2283);
    }
}
