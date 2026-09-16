package com.vaadin.demo.nordicsupply.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import com.vaadin.flow.component.ai.provider.ToolException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.vaadin.demo.nordicsupply.config.PackData;

class JdbcDatabaseProviderTest {

    private static JdbcDatabaseProvider provider;

    @BeforeAll
    static void setUp() {
        var db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("providertest")
                .build();
        var jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE widgets (id INT PRIMARY KEY, name VARCHAR(20))");
        jdbc.update("INSERT INTO widgets (id, name) VALUES (1, 'one')");
        provider = new JdbcDatabaseProvider(() -> jdbc, Optional::empty, new PackData("nordic_supply"));
    }

    @Test
    void rejectsAnythingButSelect() {
        assertThatThrownBy(() -> provider.executeQuery("DELETE FROM widgets"))
                .isInstanceOf(ToolException.class)
                .hasMessageContaining("Only SELECT statements");
    }

    @Test
    void acceptsSelect() {
        assertThat(provider.executeQuery("SELECT 1")).hasSize(1);
    }

    @Test
    void acceptsCommonTableExpression() {
        assertThat(provider.executeQuery("WITH t AS (SELECT 1) SELECT * FROM t"))
                .hasSize(1);
    }

    @Test
    void stripsTrailingSemicolons() {
        assertThat(provider.executeQuery("SELECT name FROM widgets;;")).hasSize(1);
    }

    @Test
    void deniesFileFunctions() {
        assertThatThrownBy(() -> provider.executeQuery("SELECT * FROM CSVREAD('x.csv')"))
                .isInstanceOf(ToolException.class)
                .hasMessageContaining("CSVREAD");
    }

    @Test
    void deniesScriptFunctions() {
        assertThatThrownBy(() -> provider.executeQuery("SELECT RUNSCRIPT FROM ('x.sql')"))
                .isInstanceOf(ToolException.class)
                .hasMessageContaining("RUNSCRIPT");
    }
}
