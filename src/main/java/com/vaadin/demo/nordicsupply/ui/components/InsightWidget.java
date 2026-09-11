package com.vaadin.demo.nordicsupply.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ai.chart.ChartAIController;
import com.vaadin.flow.component.ai.chart.ChartConfigurationParser;
import com.vaadin.flow.component.ai.chart.ChartState;
import com.vaadin.flow.component.ai.grid.AIDataRow;
import com.vaadin.flow.component.ai.grid.GridAIController;
import com.vaadin.flow.component.ai.grid.GridState;
import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.util.ChartSerialization;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.function.SerializableConsumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.demo.nordicsupply.ai.JdbcDatabaseProvider;
import com.vaadin.demo.nordicsupply.ai.TurnLogger;
import com.vaadin.demo.nordicsupply.ai.WidgetReply;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.domain.ActivityView;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.util.Errors;

/**
 * One Insights widget: a grid or a chart with its own chat and orchestrator. The chat lives in the view's sidebar
 * while the widget is selected. After every turn the model names the widget (a short title and a one-sentence
 * description in business terms); the widget knows its persistable state (the SQL, and for charts the
 * configuration) so the dashboard can be saved. The model sees the schema text and today's date and never a row.
 */
public class InsightWidget extends DashboardWidget {

    /** Long enough to read a provider's error message before it disappears. */
    private static final int ERROR_NOTIFICATION_MS = 8000;

    static final String SYSTEM_PROMPT =
            """
            You help an analyst answer questions about live operations by querying the database described by the schema
            tool. Always call the tools; never invent data. Read the schema first if you have not. When a question can be
            read in more than one way, say which reading you used in one sentence.
            Begin every reply with exactly two lines, then a blank line:
            TITLE: <at most six words naming what the widget shows, e.g. "Late shipments, August 2026">
            SUMMARY: <one sentence in business terms saying what the widget contains and what was counted>
            After those lines, add at most two short sentences (the reading you used, what to check). Nothing else.
            For tables: at most seven columns. Put the main entity's business key first (order_number, claim_number,
            customer_number, sku or shipment_number), prefer names to ids, and never select raw id columns.
            When grouping by day, week or month, select the period as a DATE (cast a truncated timestamp to DATE) or
            as a short label such as 'W34' or '2026-08', never as a timestamp.
            For charts: one row per point with the label column first, then one numeric column per series; at most
            thirty points; give the axes short titles and leave the chart title empty, the widget has its own.
            This widget is a %s and stays one. Only when the user asks for a %s, say in one sentence that the New
            Query box creates one beside this widget; otherwise do not mention it.
            """;

    /** The system prompt for a widget of the given type. */
    static String systemPrompt(Type type) {
        return type == Type.CHART
                ? SYSTEM_PROMPT.formatted("chart", "table")
                : SYSTEM_PROMPT.formatted("table", "chart");
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public enum Type {
        GRID("Create table", "Table"),
        CHART("Create chart", "Chart");

        private final String action;
        private final String shortLabel;

        Type(String action, String shortLabel) {
            this.action = action;
            this.shortLabel = shortLabel;
        }

        public String action() {
            return action;
        }

        public String shortLabel() {
            return shortLabel;
        }
    }

    /** What is stored for a widget: the SQL as text and, for charts, the queries and configuration as JSON. */
    public record State(String sql, String stateJson) {}

    private final Type type;
    private final Chart chart;
    private final Grid<AIDataRow> grid;
    private final GridAIController gridController;
    private final ChartAIController chartController;
    private final ChatPanel chat = new ChatPanel();
    private final Paragraph description = new Paragraph();
    private final Span note = new Span();
    private final Button edit = new Button(VaadinIcon.PENCIL.create());
    private final Icon selectedMark = VaadinIcon.CHECK_CIRCLE.create();
    private final ProgressBar progress = new ProgressBar();
    private final Span thinking = new Span("Thinking…");
    private final AIOrchestrator orchestrator;
    private Integer savedId;
    private SerializableConsumer<InsightWidget> changeListener;

    public InsightWidget(
            Type type,
            String title,
            JdbcDatabaseProvider db,
            Supplier<LLMProvider> providers,
            ActivityLog log,
            CurrentUser user,
            String model) {
        super(title);
        this.type = type;
        addClassName("insight-widget");
        Component visual;
        AIController controller;
        if (type == Type.CHART) {
            chart = new Chart();
            chart.setSizeFull();
            styled(chart.getConfiguration());
            chartController = new ChartAIController(chart, db);
            chartController.addStateChangeListener(s -> {
                // the model's configuration replaces the widget's; keep the chart on the theme's CSS colours
                if (!Boolean.TRUE.equals(chart.getConfiguration().getChart().getStyledMode())) {
                    styled(chart.getConfiguration());
                    chart.drawChart(true);
                }
                changed();
            });
            gridController = null;
            grid = null;
            controller = chartController;
            visual = chart;
        } else {
            chart = null;
            grid = new Grid<>();
            grid.setSizeFull();
            gridController = new GridAIController(grid, db);
            gridController.addStateChangeListener(s -> {
                decorateGrid();
                changed();
            });
            chartController = null;
            controller = gridController;
            visual = grid;
        }
        var logged = new TurnLogger(
                log, ActivityView.INSIGHTS, user::id, () -> savedId, "schema text + today's date; no rows", model);
        var logRequest = logged.request();
        var logResponse = logged.response();
        orchestrator = AIOrchestrator.builder(providers.get(), systemPrompt(type))
                .withMessageList(chat.messageList())
                .withInput(chat.messageInput())
                .withController(controller)
                .withRequestInterceptor(logged.interceptor())
                .withRequestListener(event -> {
                    logRequest.onRequest(event);
                    note.setText("");
                    setBusy(true);
                })
                .withResponseListener(event -> {
                    logResponse.onResponse(event);
                    onResponse(event);
                })
                .withUserName(user.get().name())
                .withAssistantName(TurnLogger.ASSISTANT_NAME)
                .build();

        description.addClassName("widget-description");
        description.setVisible(false);
        note.addClassName("note");
        edit.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        edit.setTooltipText("Rename or describe this widget");
        edit.setAriaLabel("Edit widget");
        selectedMark.addClassName("selected-mark");
        selectedMark.setVisible(false);
        var header = new HorizontalLayout(edit, selectedMark);
        header.setSpacing(false);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        setHeaderContent(header);
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.addClassName("widget-progress");
        thinking.addClassName("thinking");
        thinking.setVisible(false);
        var content = new VerticalLayout(description, note, progress, visual);
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.expand(visual);
        setContent(content);
    }

    /** The gold check on the selected widget. */
    public void setSelectedMark(boolean selected) {
        selectedMark.setVisible(selected);
    }

    /** "Thinking…", shown in the sidebar chat while a turn runs; owned by the widget so it follows its chat. */
    public Span thinkingIndicator() {
        return thinking;
    }

    public Type type() {
        return type;
    }

    /** The widget's chat; the view shows it in the sidebar while the widget is selected. */
    public ChatPanel chat() {
        return chat;
    }

    public Integer savedId() {
        return savedId;
    }

    public void setSavedId(Integer savedId) {
        this.savedId = savedId;
    }

    public String description() {
        return description.getText();
    }

    public void setDescription(String text) {
        description.setText(text == null ? "" : text);
        description.setVisible(text != null && !text.isBlank());
    }

    public void onEdit(SerializableConsumer<InsightWidget> handler) {
        edit.addClickListener(e -> handler.accept(this));
    }

    /**
     * Called after every successful turn that changed the widget, and when the model renamed it (not
     * after a restore).
     */
    public void onChanged(SerializableConsumer<InsightWidget> listener) {
        this.changeListener = listener;
    }

    /** Sends a question to this widget's chat. */
    public void ask(String question) {
        orchestrator.prompt(question);
    }

    /** The persistable state, or {@code null} while the widget has not shown a result. */
    public State state() {
        var json = JSON.createObjectNode();
        json.put("colspan", getColspan());
        json.put("rowspan", getRowspan());
        if (gridController != null) {
            var s = gridController.getState();
            return s == null ? null : new State(s.query(), json.toString());
        }
        var s = chartController.getState();
        if (s == null) {
            return null;
        }
        var queries = json.putArray("queries");
        s.queries().forEach(queries::add);
        json.set("configuration", JSON.readTree(ChartSerialization.toJSON(s.configuration())));
        return new State(String.join("\n\n", s.queries()), json.toString());
    }

    /**
     * Restores a saved widget: re-runs the query; a chart without a stored configuration gets a
     * default column look.
     */
    public void restore(String sql, String stateJson) {
        if (sql == null || sql.isBlank()) {
            return;
        }
        JsonNode json = stateJson == null || stateJson.isBlank() ? null : JSON.readTree(stateJson);
        if (json != null && json.has("colspan")) {
            setColspan(Math.max(1, json.get("colspan").asInt(1)));
            setRowspan(Math.max(1, json.get("rowspan").asInt(1)));
        }
        if (gridController != null) {
            gridController.restoreState(new GridState(sql));
            decorateGrid();
        } else {
            List<String> queries = new ArrayList<>();
            Configuration configuration;
            if (json != null && json.has("queries")) {
                json.get("queries").forEach(q -> queries.add(q.asString()));
                configuration =
                        ChartConfigurationParser.parse(json.get("configuration").toString());
            } else {
                queries.add(sql);
                configuration = new Configuration();
                configuration.getChart().setType(ChartType.COLUMN);
                configuration.setTitle("");
                note.setText("Restored with a default chart style; ask the chat to restyle it.");
            }
            styled(configuration);
            chartController.restoreState(new ChartState(queries, configuration));
        }
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        thinking.setVisible(busy);
    }
    /**
     * After every render: hide raw id columns the model may still have selected. The renderer's own columns and
     * headers stay as they are; the rows belong to the controller and are not read here.
     */
    private void decorateGrid() {
        if (grid == null) {
            return;
        }
        for (var column : grid.getColumns()) {
            var key = column.getKey();
            if (key != null && isIdColumn(key)) {
                column.setVisible(false);
            }
        }
    }

    private static boolean isIdColumn(String key) {
        var k = key.toLowerCase();
        return k.equals("id") || k.endsWith("_id") || k.endsWith(".id");
    }

    private void changed() {
        if (changeListener != null) {
            changeListener.accept(this);
        }
    }

    /** Runs on the provider's thread: anything that touches the UI goes through {@code ui.access}. */
    private void onResponse(ResponseListener.ResponseEvent event) {
        boolean failed = event.getError().isPresent();
        getUI().ifPresent(ui -> ui.access(() -> setBusy(false)));
        if (failed) {
            var message = "The model call failed: "
                    + Errors.rootMessage(event.getError().get(), 300);
            getUI().ifPresent(ui -> ui.access(() -> {
                var n = Notification.show(message, ERROR_NOTIFICATION_MS, Notification.Position.BOTTOM_START);
                n.addThemeVariants(NotificationVariant.ERROR);
            }));
            return;
        }
        var response = event.getResponse() == null ? "" : event.getResponse();
        var reply = WidgetReply.parse(response);
        if (reply.title().isEmpty() && reply.summary().isEmpty()) {
            return;
        }
        getUI().ifPresent(ui -> ui.access(() -> {
            if (!reply.title().isBlank()) {
                setTitle(reply.title());
            }
            if (!reply.summary().isBlank()) {
                setDescription(reply.summary());
            }
            var items = chat.messageList().getItems();
            if (!items.isEmpty()
                    && TurnLogger.ASSISTANT_NAME.equals(items.getLast().getUserName())) {
                items.getLast()
                        .setText(
                                reply.rest().isEmpty()
                                        ? (reply.summary().isEmpty() ? reply.title() : reply.summary())
                                        : reply.rest());
            }
            changed();
        }));
    }

    /** Vaadin Charts draws with Highcharts' own light theme unless styled mode is on; the demo's CSS styles it. */
    private static void styled(Configuration configuration) {
        configuration.getChart().setStyledMode(true);
        // no axis title unless the model set one: Highcharts would otherwise print "Values"
        var yAxis = configuration.getyAxis();
        if (yAxis.getTitle() == null || yAxis.getTitle().getText() == null) {
            yAxis.setTitle("");
        }
    }
}
