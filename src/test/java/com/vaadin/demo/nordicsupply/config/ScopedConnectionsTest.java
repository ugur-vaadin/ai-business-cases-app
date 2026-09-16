package com.vaadin.demo.nordicsupply.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.demo.nordicsupply.ai.JdbcDatabaseProvider;
import com.vaadin.demo.nordicsupply.session.Scopes;

/** What each role's connection can read: its own countries, through the filtered views and nothing else. */
@SpringBootTest
@ActiveProfiles("test")
class ScopedConnectionsTest {

    @Autowired
    private ScopedConnections connections;

    @Autowired
    private PackData pack;

    @Test
    void aRoleSeesOnlyItsOwnCountries() {
        assertThat(connections
                        .templateFor(Scopes.FI_EE)
                        .queryForList("SELECT DISTINCT country FROM customers ORDER BY country", String.class))
                .containsExactly("EE", "FI");
        assertThat(connections
                        .templateFor(Scopes.ALL)
                        .queryForList("SELECT DISTINCT country FROM customers ORDER BY country", String.class))
                .hasSize(6);
    }

    @Test
    void everythingThatHangsOffACustomerIsScopedTheSameWay() {
        var scoped = connections.templateFor(Scopes.FI_EE).queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        var expected = connections
                .templateFor(Scopes.ALL)
                .queryForObject(
                        "SELECT COUNT(*) FROM orders o JOIN customers c ON c.id = o.customer_id "
                                + "WHERE c.country IN ('FI','EE')",
                        Long.class);
        assertThat(scoped).isEqualTo(expected);
    }

    @Test
    void theBaseTablesAndTheMembershipTableAreOutOfReach() {
        var fiEe = connections.templateFor(Scopes.FI_EE);
        assertThatThrownBy(() -> fiEe.queryForObject("SELECT COUNT(*) FROM public.customers", Long.class))
                .hasMessageContaining("PUBLIC.CUSTOMERS");
        assertThatThrownBy(() -> fiEe.queryForObject("SELECT COUNT(*) FROM public.scope_membership", Long.class))
                .hasMessageContaining("PUBLIC.SCOPE_MEMBERSHIP");
    }

    @Test
    void theCountryFilterNarrowsWithinTheRoleAndIsResetAfterwards() {
        var country = new String[] {"FI"};
        var provider = new JdbcDatabaseProvider(
                () -> connections.templateFor(Scopes.FI_EE), () -> Optional.ofNullable(country[0]), pack);
        assertThat(provider.executeQuery("SELECT DISTINCT country FROM customers"))
                .hasSize(1);

        country[0] = "SE";
        assertThat(provider.executeQuery("SELECT DISTINCT country FROM customers"))
                .isEmpty();

        country[0] = null;
        assertThat(provider.executeQuery("SELECT DISTINCT country FROM customers ORDER BY country"))
                .hasSize(2);
    }

    @Test
    void onlyABusinessRoleHasAConnection() {
        assertThatThrownBy(() -> connections.templateFor(Scopes.FI))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not a role scope: fi");
    }
}
