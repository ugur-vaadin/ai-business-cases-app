package com.vaadin.demo.nordicsupply.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Two connections to one in-memory H2 database: the application's own ({@code sa}) and the AI's read-only account
 * ({@code ai_reader}, created by the pack's script, which can see the exposed tables and views and nothing else), so a
 * model-written query can never change data. The pack's CSVs are loaded at start-up through H2's CSVREAD from the
 * classpath, in the order of the pack's own loader script. The tables the application owns and the sequences its
 * inserts draw from come from Flyway ({@code db/migration}), which runs once this data source exists; that is why the
 * load happens inside the data source's factory method and not in a runner that would start after Flyway.
 */
@Configuration
public class DataConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DataConfig.class);
    private static final String URL = "jdbc:h2:mem:aicases;DB_CLOSE_DELAY=-1";

    /** Model-written queries are cut off after this; a runaway join must not hold a thread. */
    private static final int AI_QUERY_TIMEOUT_SECONDS = 15;

    @Bean
    public PackData packData(@Value("${app.pack}") String pack) {
        return new PackData(pack);
    }

    @Bean
    @Primary
    public DataSource appDataSource(PackData pack) throws IOException, SQLException {
        var ds = pooled("sa", "", URL);
        load(ds, pack);
        return ds;
    }

    /** Two {@code JdbcTemplate}s exist, so the application's is declared explicitly and marked primary. */
    @Bean
    @Primary
    public JdbcTemplate appJdbc(DataSource appDataSource) {
        return new JdbcTemplate(appDataSource);
    }

    /** The AI's connection: same database, the read-only account. */
    @Bean
    @AiDatabase
    public DataSource aiDataSource(DataSource appDataSource) {
        // no DB_CLOSE_DELAY here: H2 runs it as SET on connect, which needs admin rights; the primary connection keeps
        // the database alive
        return pooled("ai_reader", "ai_reader", URL.substring(0, URL.indexOf(';')));
    }

    /** The template the model's queries run on; it gives up on a query that runs too long. */
    @Bean
    @AiDatabase
    public JdbcTemplate aiJdbc(@AiDatabase DataSource aiDataSource) {
        var template = new JdbcTemplate(aiDataSource);
        template.setQueryTimeout(AI_QUERY_TIMEOUT_SECONDS);
        return template;
    }

    private static HikariDataSource pooled(String user, String password, String url) {
        var ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setPoolName(user);
        ds.setMaximumPoolSize(8);
        return ds;
    }

    private void load(DataSource ds, PackData pack) throws IOException, SQLException {
        LOG.info("loading pack {} into H2", pack.pack());
        long t0 = System.currentTimeMillis();
        try (var con = ds.getConnection()) {
            ScriptUtils.executeSqlScript(con, pack.resource("sql/schema-h2.sql"));
            // the pack's loader script expects the CSVs at classpath:/data/<table>.csv; this application keeps them per
            // pack
            var loader = pack.read("sql/load-h2-classpath.sql")
                    .replace("classpath:/data/", "classpath:/data/" + pack.pack() + "/csv/");
            ScriptUtils.executeSqlScript(
                    con,
                    new EncodedResource(
                            new ByteArrayResource(loader.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(con, pack.resource("sql/readonly-user-h2.sql"));
            // the user selector reads the pack's staff view; without it the app would fail on the first save
            try (var views = con.getMetaData().getTables(null, null, "STAFF", new String[] {"VIEW"})) {
                if (!views.next()) {
                    throw new IllegalStateException(
                            "pack " + pack.pack() + " has no staff view; the user selector needs one");
                }
            }
        }
        LOG.info("pack {} loaded into H2 in {} ms", pack.pack(), System.currentTimeMillis() - t0);
    }
}
