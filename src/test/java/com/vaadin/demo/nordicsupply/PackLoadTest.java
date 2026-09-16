package com.vaadin.demo.nordicsupply;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.demo.nordicsupply.data.OrderDeskStats;
import com.vaadin.demo.nordicsupply.data.SavedWidgets;

/** The pack really loads into H2, the migrations run on top of it, and the classes that read it see the seeded data. */
@SpringBootTest
@ActiveProfiles("test")
class PackLoadTest {

    @Autowired
    private SavedWidgets widgets;

    @Autowired
    private OrderDeskStats stats;

    @Test
    void everyDemoUserHasASavedDashboard() {
        // the pack's two widgets plus the application's migration for Test User; two or three per account manager
        assertThat(widgets.forUser(1)).hasSize(4);
        assertThat(widgets.forUser(8)).hasSize(3);
        assertThat(widgets.forUser(9)).hasSize(3);
        assertThat(widgets.forUser(10)).hasSize(2);
        assertThat(widgets.forUser(2)).isEmpty();
    }

    @Test
    void theHomeTilesCountWhatTheManualCaseSays() {
        var tiles = stats.tiles(LocalDate.of(2026, 9, 7));
        assertThat(tiles).hasSize(5);
        assertThat(tiles.stream().map(OrderDeskStats.Tile::value)).containsExactly(55, 67, 361, 3060, 2283);
    }
}
