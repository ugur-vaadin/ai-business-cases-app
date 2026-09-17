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
 * The application's own connection to the in-memory H2 database ({@code sa}). The pack's CSVs are loaded at start-up
 * through H2's CSVREAD from the classpath, in the order of the pack's own loader script. The tables the application
 * owns, the sequences its inserts draw from and the filtered views the model reads come from Flyway
 * ({@code db/migration}), which runs once this data source exists; that is why the load happens inside the data
 * source's factory method and not in a runner that would start after Flyway. The model's queries run on the
 * read-only accounts of {@link ScopedConnections}, never on this connection.
 */
@Configuration
public class DataConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DataConfig.class);
    /** The in-memory database every connection of the application opens. */
    static final String DATABASE = "jdbc:h2:mem:aicases";

    private static final String URL = DATABASE + ";DB_CLOSE_DELAY=-1";

    /** The application's own pool, shared by the read-only views and the claim form; enough for one demo session. */
    private static final int POOL_SIZE = 8;

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

    /** The application's own template; the model's queries run on the scoped connections instead. */
    @Bean
    @Primary
    public JdbcTemplate appJdbc(DataSource appDataSource) {
        return new JdbcTemplate(appDataSource);
    }

    private static HikariDataSource pooled(String user, String password, String url) {
        var ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setPoolName(user);
        ds.setMaximumPoolSize(POOL_SIZE);
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
