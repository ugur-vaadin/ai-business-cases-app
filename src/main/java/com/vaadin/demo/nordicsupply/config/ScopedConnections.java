package com.vaadin.demo.nordicsupply.config;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.vaadin.demo.nordicsupply.session.Scope;
import com.vaadin.demo.nordicsupply.session.Scopes;

/**
 * The database connections the model's queries run on: one read-only account per business role, each of which can
 * read only the filtered views of the {@code scoped} schema. A query therefore returns the rows of the signed-in
 * user's countries because the database says so, not because the question was phrased that way.
 * <p>
 * The accounts are created by the migration with a password that does not work; this service replaces each of them
 * at start-up with a random value it keeps in memory only, so no usable credential exists in the sources or on disk.
 */
@Service
public class ScopedConnections {

    /** Model-written queries are cut off after this; a runaway join must not hold a thread. */
    private static final int AI_QUERY_TIMEOUT_SECONDS = 15;

    /** The model's connections see the filtered views, so they open on that schema. */
    private static final String URL = DataConfig.DATABASE + ";SCHEMA=scoped";

    private static final String USER_PREFIX = "ai_reader_";

    private static final int PASSWORD_LENGTH = 32;

    private static final String PASSWORD_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int POOL_SIZE = 4;

    private final DataSource appDataSource;
    private final SecureRandom random;
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    @Autowired
    public ScopedConnections(DataSource appDataSource) {
        this(appDataSource, new SecureRandom());
    }

    public ScopedConnections(DataSource appDataSource, SecureRandom random) {
        this.appDataSource = appDataSource;
        this.random = random;
    }

    @PostConstruct
    void replacePasswords() {
        var admin = new JdbcTemplate(appDataSource);
        for (var scope : roleScopes()) {
            var user = userFor(scope);
            var password = randomPassword();
            // the value is bound, never concatenated, and never logged
            admin.update("ALTER USER " + user + " SET PASSWORD ?", password);
            passwords.put(scope.key(), password);
        }
    }

    /**
     * The connection of a business role. Every query on it sees the countries of that role and nothing else.
     *
     * @throws IllegalArgumentException when the scope is not one of the four business roles
     */
    public JdbcTemplate templateFor(Scope roleScope) {
        var key = roleScope.key();
        if (Scopes.roleScope(key).isEmpty()) {
            throw new IllegalArgumentException("not a role scope: " + key);
        }
        var template = new JdbcTemplate(pools.computeIfAbsent(key, this::pool));
        template.setQueryTimeout(AI_QUERY_TIMEOUT_SECONDS);
        return template;
    }

    @PreDestroy
    void closePools() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }

    private HikariDataSource pool(String key) {
        var user = USER_PREFIX + key;
        var ds = new HikariDataSource();
        ds.setJdbcUrl(URL);
        ds.setUsername(user);
        ds.setPassword(passwords.get(key));
        ds.setPoolName(user);
        ds.setMaximumPoolSize(POOL_SIZE);
        ds.setReadOnly(true);
        // a pooled connection starts without a country filter even if a reset was missed
        ds.setConnectionInitSql("SET @country = NULL");
        return ds;
    }

    private static Iterable<Scope> roleScopes() {
        return Scopes.all().stream()
                .filter(scope -> Scopes.roleScope(scope.key()).isPresent())
                .toList();
    }

    private static String userFor(Scope roleScope) {
        return USER_PREFIX + roleScope.key();
    }

    private String randomPassword() {
        var out = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            out.append(PASSWORD_CHARACTERS.charAt(random.nextInt(PASSWORD_CHARACTERS.length())));
        }
        return out.toString();
    }
}
