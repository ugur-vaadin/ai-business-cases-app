package com.vaadin.demo.nordicsupply.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.vaadin.flow.component.ai.provider.DatabaseProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.util.Errors;

/**
 * What the model may see: the pack's schema text plus today's date, and query results through the read-only account
 * of the signed-in user's role. Rows go to the grid or chart; the model receives only the schema text and the tool's
 * confirmation. The grid controller pages its query itself (LIMIT/OFFSET and a COUNT around the model's SQL); the
 * chart controller reads the whole result, so the prompt asks for a bounded number of points and the connection has
 * a query timeout.
 * <p>
 * Which rows a query returns is decided by the database: the account may read only views that filter by the
 * countries of its role, and the optional country filter is applied by the same views through a session variable
 * that is set and cleared around every query.
 */
public class JdbcDatabaseProvider implements DatabaseProvider {

    /**
     * H2 functions that read or write files or run scripts. Refusing them is not the security control: the grants
     * and the non-administrative account are, and they would refuse these calls anyway. The list exists so that the
     * model reads one clear sentence instead of a database error and can correct itself in the same turn.
     */
    public static final Set<String> DENIED_FUNCTIONS = Set.of(
            "FILE_READ",
            "FILE_WRITE",
            "CSVREAD",
            "CSVWRITE",
            "RUNSCRIPT",
            "SCRIPT",
            "LINK_SCHEMA",
            "TABLE",
            "TABLE_DISTINCT",
            "SYSTEM_RANGE");

    private static final Pattern DENIED_CALL =
            Pattern.compile("\\b(" + String.join("|", DENIED_FUNCTIONS) + ")\\s*\\(", Pattern.CASE_INSENSITIVE);

    /** The two statement kinds this connection runs; a query has to start with one of them. */
    private static final String SELECT = "SELECT";

    private static final String WITH = "WITH";

    /** How much of the database's message the model is given back; enough to correct the query, not a whole dump. */
    private static final int ERROR_MESSAGE_LENGTH = 400;

    private final transient Supplier<JdbcTemplate> connection;
    private final transient Supplier<Optional<String>> country;
    private final transient PackData pack;

    public JdbcDatabaseProvider(Supplier<JdbcTemplate> connection, Supplier<Optional<String>> country, PackData pack) {
        this.connection = connection;
        this.country = country;
        this.pack = pack;
    }

    @Override
    public String getSchema() {
        return pack.schemaTextForToday();
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        var trimmed = sql.trim().replaceAll(";+$", "");
        if (!trimmed.regionMatches(true, 0, SELECT, 0, SELECT.length())
                && !trimmed.regionMatches(true, 0, WITH, 0, WITH.length())) {
            throw new ToolException("Only SELECT statements are allowed on this connection.");
        }
        var denied = DENIED_CALL.matcher(trimmed);
        if (denied.find()) {
            throw new ToolException("The function " + denied.group(1).toUpperCase(Locale.ROOT)
                    + " is not available on this connection.");
        }
        // both suppliers are read here, so switching the user or the country filter takes effect on the next query
        var template = connection.get();
        var filter = country.get();
        try {
            return template.execute((ConnectionCallback<List<Map<String, Object>>>) con -> {
                try {
                    setCountry(con, filter.orElse(null));
                    // model-written SQL runs here on purpose: the account may read only the filtered views, so
                    // anything else fails at the database; the guards above only give the model a clearer message
                    try (var statement = con.prepareStatement(trimmed)) {
                        if (template.getQueryTimeout() > 0) {
                            statement.setQueryTimeout(template.getQueryTimeout());
                        }
                        try (var rs = statement.executeQuery()) {
                            var mapper = new ColumnMapRowMapper();
                            var rows = new ArrayList<Map<String, Object>>();
                            int number = 0;
                            while (rs.next()) {
                                rows.add(mapper.mapRow(rs, number++));
                            }
                            return rows;
                        }
                    }
                } finally {
                    // a pooled connection must never carry a filter into the next query
                    setCountry(con, null);
                }
            });
        } catch (Exception e) {
            // the model gets the database's message and can correct the query in the same turn
            throw new ToolException("Query failed: " + Errors.rootMessage(e, ERROR_MESSAGE_LENGTH));
        }
    }

    private static void setCountry(java.sql.Connection con, String code) throws java.sql.SQLException {
        if (code == null) {
            try (var statement = con.createStatement()) {
                statement.execute("SET @country = NULL");
            }
        } else {
            try (var statement = con.prepareStatement("SET @country = ?")) {
                statement.setString(1, code);
                statement.execute();
            }
        }
    }
}
