package com.vaadin.demo.nordicsupply.ai;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.ai.provider.DatabaseProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.util.Errors;

/**
 * What the model may see: the pack's schema text plus today's date, and query results through the read-only
 * account. Rows go to the grid or chart; the model receives only the schema text and the tool's confirmation. The
 * grid controller pages its query itself (LIMIT/OFFSET and a COUNT around the model's SQL); the chart controller
 * reads the whole result, so the prompt asks for a bounded number of points and the connection has a query timeout.
 */
public class JdbcDatabaseProvider implements DatabaseProvider {

    private final transient JdbcTemplate aiJdbc;
    private final transient PackData pack;

    public JdbcDatabaseProvider(JdbcTemplate aiJdbc, PackData pack) {
        this.aiJdbc = aiJdbc;
        this.pack = pack;
    }

    @Override
    public String getSchema() {
        return pack.schemaTextForToday();
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        var trimmed = sql.trim().replaceAll(";+$", "");
        if (!trimmed.regionMatches(true, 0, "SELECT", 0, 6) && !trimmed.regionMatches(true, 0, "WITH", 0, 4)) {
            throw new ToolException("Only SELECT statements are allowed on this connection.");
        }
        try {
            // model-written SQL runs here on purpose: the connection is the read-only account, so anything but a
            // SELECT fails at the database; the guard above only gives the model a clearer message
            return aiJdbc.queryForList(trimmed);
        } catch (Exception e) {
            // the model gets the database's message and can correct the query in the same turn
            throw new ToolException("Query failed: " + Errors.rootMessage(e, 400));
        }
    }
}
