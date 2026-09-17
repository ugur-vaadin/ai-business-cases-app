package com.vaadin.demo.nordicsupply.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.demo.nordicsupply.ai.JdbcDatabaseProvider;
import com.vaadin.demo.nordicsupply.config.ModelSettings;
import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.config.ScopedConnections;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.data.SavedWidgets;
import com.vaadin.demo.nordicsupply.domain.ActivityView;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.CountryFilter;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.InsightWidget;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.QueryPanel;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.ui.components.WidgetEditDialog;

/**
 * The self-service dashboard ("Insights"). An empty dashboard shows one prompt in the middle of the page. A question
 * adds a grid or chart widget and opens its chat in a popover beside it; the chat icon in a widget's header opens it
 * again. The last tile of the dashboard is "New Query" and holds the same prompt, so the next question is typed
 * where the next widget will appear. The popover closes with Escape or its close button. Widgets can be renamed,
 * moved, resized and removed; "Save Dashboard" keeps what is on screen (inserting, updating and deleting rows to
 * match). Dashboards are personal.
 * <p>
 * The dashboard's own selection (the state its move, resize and remove controls need) follows keyboard focus and is
 * cleared as soon as focus leaves the widget, which it does when the user types into the chat. The view therefore
 * keeps its own notion of the <em>active</em> widget, set when its chat opens, and never reads the dashboard's
 * selection.
 */
@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights")
public class InsightsView extends VerticalLayout implements HasReadme {

    /** Three tiles across, as designed; the popover beside a widget is about one tile wide. */
    private static final int MAX_COLUMNS = 3;

    /** Narrower than this and a chart's axis labels collide; three columns fit from about 1100px. */
    private static final String MIN_COLUMN_WIDTH = "260px";

    /** The widgets' proportions as designed: about 4:3 at three columns on a 1440px screen. */
    private static final String ROW_HEIGHT = "300px";

    private static final String GAP = "1rem";

    /** The popover beside a widget: as wide as a one-column widget, and tall enough for a few turns of chat. */
    private static final String POPOVER_WIDTH = "400px";

    private static final String POPOVER_HEIGHT = "500px";

    static final Readme README = new Readme(
            "Insights",
            "Ask a question about live operations; the answer becomes a widget you can keep.",
            """
            ## What this page does

            You are an analyst on the **Nordic Supply** order desk. Type a question about orders, shipments, claims or
            products, choose a table or a chart, and press the arrow. The answer appears as a widget on your dashboard,
            named and described by the assistant in business terms. The speech-bubble icon in a widget's header opens
            its chat beside it, so you can refine it: "group it by week", "only the Göteborg warehouse", "add the
            customer". A table stays a table and a chart stays a chart; the **New Query** tile at the end of the
            dashboard asks the next question.

            ## Things to try

            * *Which orders shipped after the promised date last month, by customer*
            * *Late shipments last month, by week* with the type set to **Chart**, then in its chat: *make it a line
              chart*
            * *Which carrier delivered late most often last month?*
            * *Claims open over 7 days*

            ## How you know the answer is right

            Every widget says what it contains and what was counted. Ask its chat which data the answer is based on and
            how it was worked out. When a question can be read in two ways, the assistant tells you which reading it
            used.

            ## What the model sees

            Only the database schema and today's date. The rows you see never leave your network: the model writes a
            query, the application runs it on a read-only account, and the result goes straight to the widget.

            ## Keeping your dashboard

            Most questions are asked once. Widgets you keep on screen, with their size and order, are stored when you
            press **Save Dashboard**; until then a reload discards them. The pencil on a widget renames or describes it;
            the speech-bubble icon opens its chat. Saved widgets come back with the same query the next time you open
            Insights. Every question and every decision is recorded in the Activity log.
            """);
    private static final int CHIPS_SHOWN = 3;

    /** Suggested questions for a chart; the table ones come from the pack, these fit a chart's shape. */
    private static final List<String> CHART_CHIPS =
            List.of("Late shipments last month, by week", "Orders per month this year", "Open claims by type");

    private final PackData pack;
    private final ScopedConnections connections;
    private final Supplier<LLMProvider> providers;
    private final ActivityLog log;
    private final CurrentUser user;
    private final SavedWidgets savedWidgets;
    private final String model;

    private final Dashboard dashboard = new Dashboard();
    private final QueryPanel query;
    private final Button saveDashboard = new Button("Save Dashboard", e -> saveDashboard());
    private final Div empty = new Div();
    private final Div scroller = new Div(dashboard);
    private final DashboardWidget newQuery = new DashboardWidget();

    /** The New Query tile's content: a title and, while the dashboard has widgets, the prompt. */
    private final Div newQueryBox = new Div();

    private final Popover popover = new Popover();
    private final Set<Integer> storedIds = new HashSet<>();
    private InsightWidget active;
    private boolean dirty;

    public InsightsView(
            PackData pack,
            ScopedConnections connections,
            Supplier<LLMProvider> providers,
            ActivityLog log,
            CurrentUser user,
            SavedWidgets savedWidgets,
            ModelSettings ai) {
        this.pack = pack;
        this.connections = connections;
        this.providers = providers;
        this.log = log;
        this.user = user;
        this.savedWidgets = savedWidgets;
        this.model = ai.model();
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("page", "insights");

        var heading = new PageHeading("Insights", "Understand your operations");
        var spacer = new Div();
        var countryFilter = new CountryFilter(user.scope(), user.filter());
        // a user responsible for one country has nothing to narrow
        countryFilter.setVisible(user.scope().countries().size() > 1);
        countryFilter.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                user.setFilter(e.getValue());
                widgets().forEach(InsightWidget::rerun);
            }
        });
        var header = new HorizontalLayout(heading, spacer, countryFilter, saveDashboard);
        header.addClassName("insights-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.START);
        header.expand(spacer);

        var tableChips = pack.declaration().dashboardChips();
        query = new QueryPanel(
                Map.of(
                        QueryPanel.Ask.TABLE,
                        tableChips.subList(0, Math.min(CHIPS_SHOWN, tableChips.size())),
                        QueryPanel.Ask.CHART,
                        CHART_CHIPS),
                this::ask);

        dashboard.addClassName("insights-dashboard");
        dashboard.setWidthFull();
        dashboard.setEditable(true);
        dashboard.setMaximumColumnCount(MAX_COLUMNS);
        dashboard.setMinimumColumnWidth(MIN_COLUMN_WIDTH);
        dashboard.setRowHeight(ROW_HEIGHT);
        dashboard.setGap(GAP);
        dashboard.setItemRemoveHandler(e -> {
            if (e.getItem() instanceof InsightWidget w) {
                remove(w);
            }
        });
        dashboard.addItemMovedListener(e -> {
            markDirty();
            keepNewQueryLast();
        });
        dashboard.addItemResizedListener(e -> markDirty());

        var tileTitle = new Span("New Query");
        tileTitle.addClassName("tile-title");
        newQueryBox.add(tileTitle);
        newQueryBox.addClassName("new-query-box");
        newQuery.setContent(newQueryBox);
        newQuery.addClassName("new-query-widget");
        // The dashboard is editable at all times and has no per-widget switch, so the tile opts out itself. Its
        // header, with the move and remove buttons, is hidden by CSS. The drag is refused here, before the dashboard
        // sees it. A move by keyboard is undone by keepNewQueryLast and a keyboard remove is ignored by the remove
        // handler, so neither needs blocking.
        newQuery.getElement()
                .addEventListener("dragstart", e -> {})
                .preventDefault()
                .stopPropagation();
        // An editable widget puts an invisible cover over its content that selects the widget on click. It lives in
        // the widget's shadow root without a part, so no Flow or CSS API reaches it; this is the one place the view
        // runs a script. With clicks passing through, a click on the prompt focuses the prompt.
        newQuery.getElement()
                .executeJs(
                        """
                        this.updateComplete.then(() => {
                            this.shadowRoot.getElementById('focus-button-wrapper').style.pointerEvents = 'none';
                        });
                        """);

        popover.setOpenOnClick(false);
        popover.setModal(false);
        popover.setCloseOnOutsideClick(false);
        popover.setPosition(PopoverPosition.END_TOP);
        popover.setWidth(POPOVER_WIDTH);
        popover.setHeight(POPOVER_HEIGHT);
        popover.setAutofocus(true);
        popover.getElement().getClassList().add("insight-popover");
        popover.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                setActive(null); // Escape on the client, or a close from the view: either way nothing is active
            }
        });
        empty.addClassName("insights-empty");
        empty.setWidthFull();
        scroller.addClassName("page-scroll");
        scroller.setWidthFull(); // a vertical layout gives its children their own width unless told otherwise

        add(
                header,
                empty,
                scroller,
                popover,
                new ReadmePopup(README, pack.declaration().company()));
        expand(scroller);
        loadSaved();
        layout();
    }

    @Override
    public Readme readme() {
        return README;
    }

    private void loadSaved() {
        for (var saved : savedWidgets.forUser(user.id())) {
            var widget = newWidget(InsightWidget.Type.valueOf(saved.type()), saved.title());
            widget.setSavedId(saved.id());
            widget.setDescription(saved.description());
            storedIds.add(saved.id());
            dashboard.add(widget);
            widget.restore(saved.sql(), saved.stateJson());
        }
        saveDashboard.setEnabled(false);
    }

    /**
     * Empty: the prompt in the middle, nothing else. With widgets: the dashboard, ending with the New Query tile that
     * holds the prompt. Save Dashboard shows while there is something to save: any widget, or the removal of the last
     * one.
     */
    private void layout() {
        boolean hasWidgets = dashboard.getWidgets().stream().anyMatch(InsightWidget.class::isInstance);
        empty.setVisible(!hasWidgets);
        scroller.setVisible(hasWidgets);
        saveDashboard.setVisible(hasWidgets || dirty);
        if (!hasWidgets) {
            popover.close();
            dashboard.remove(newQuery);
            query.setMode(QueryPanel.Mode.CENTER);
            empty.removeAll();
            empty.add(query);
        } else {
            if (!dashboard.getWidgets().contains(newQuery)) {
                dashboard.add(newQuery);
            }
            if (query.getParent().orElse(null) != newQueryBox) {
                query.setMode(QueryPanel.Mode.TILE);
                newQueryBox.add(query); // moves it out of the empty state
            }
        }
    }

    /** The New Query tile is always the last one; a move that put it elsewhere is undone. */
    private void keepNewQueryLast() {
        var widgets = dashboard.getWidgets();
        if (widgets.contains(newQuery) && widgets.getLast() != newQuery) {
            dashboard.remove(newQuery);
            dashboard.add(newQuery);
        }
    }

    /** Opens the popover with the widget's chat beside it. */
    private void activate(InsightWidget widget) {
        if (widget == active && popover.isOpened()) {
            return;
        }
        setActive(widget);
        var footnote = new Span(QueryPanel.FOOTNOTE);
        footnote.addClassName("footnote");
        var content = new Div();
        // the widget itself shows only its title; what it contains, in business terms, is said here
        var intro = new Span(chatIntro(widget));
        intro.addClassName("chat-intro");
        content.add(intro, widget.chat(), widget.thinkingIndicator(), footnote);
        content.addClassName("popover-chat");
        var close = new Button(VaadinIcon.CLOSE.create(), e -> popover.close());
        close.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        close.addClassName("popover-close");
        close.setAriaLabel("Close");
        popover.removeAll();
        popover.add(close, content);
        if (popover.getTarget() != widget) {
            popover.setTarget(widget);
        }
        openAfterRender();
    }

    /**
     * Opens the popover once the browser has laid the dashboard out. Adding or moving a tile makes the dashboard
     * re-render its cells in the same round trip; a popover opened in that round trip is closed again by the client
     * because its target moved, and the closing would also clear the active widget. Waiting two frames avoids that.
     */
    private void openAfterRender() {
        // opened on the client, where the frames are; the property syncs back to the server
        popover.getElement()
                .executeJs("requestAnimationFrame(() => requestAnimationFrame(() => { this.opened = true; }))");
    }

    /** The model's description of the widget, a restore note if there is one, or a hint when the chat is still empty. */
    private static String chatIntro(InsightWidget widget) {
        var parts = new StringBuilder();
        if (!widget.description().isBlank()) {
            parts.append(widget.description());
        }
        if (!widget.note().isBlank()) {
            parts.append(parts.isEmpty() ? "" : " ").append(widget.note());
        }
        if (parts.isEmpty() && widget.chat().messageList().getItems().isEmpty()) {
            parts.append("Ask this widget to refine itself: group it differently, add a column, narrow the period, "
                    + "or ask which data it is based on.");
        }
        return parts.toString();
    }

    /** Marks the active widget (gold outline and check) and clears the previous one. */
    private void setActive(InsightWidget widget) {
        if (active != null && active != widget) {
            active.removeClassName("active");
            active.setSelectedMark(false);
        }
        active = widget;
        if (widget != null) {
            widget.addClassName("active");
            widget.setSelectedMark(true);
        }
    }

    /** A question adds one widget of the chosen kind in front of the New Query tile and asks it there. */
    private void ask(String question, QueryPanel.Ask choice) {
        var type = choice == QueryPanel.Ask.CHART ? InsightWidget.Type.CHART : InsightWidget.Type.GRID;
        var widget = newWidget(type, question);
        int index = dashboard.getWidgets().indexOf(newQuery);
        if (index < 0) {
            dashboard.add(widget);
        } else {
            dashboard.addWidgetAtIndex(index, widget);
        }
        markDirty();
        layout();
        activate(widget);
        widget.ask(question);
    }

    private InsightWidget newWidget(InsightWidget.Type type, String title) {
        var db =
                new JdbcDatabaseProvider(() -> connections.templateFor(user.scope()), () -> user.filterCountry(), pack);
        var widget = new InsightWidget(type, title, db, providers, log, user, model);
        widget.onAsk(this::activate);
        widget.onEdit(this::editDialog);
        widget.onChanged(w -> markDirty());
        return widget;
    }

    private void editDialog(InsightWidget widget) {
        new WidgetEditDialog(widget, this::markDirty).open();
    }

    private void remove(InsightWidget widget) {
        if (widget == active) {
            popover.close();
        }
        dashboard.remove(widget);
        markDirty();
        layout();
    }

    /** Makes the stored dashboard match the screen: insert new widgets, update kept ones, delete removed ones. */
    private void saveDashboard() {
        var kept = new HashSet<Integer>();
        int position = 0;
        int skipped = 0;
        for (DashboardWidget w : dashboard.getWidgets()) {
            if (!(w instanceof InsightWidget widget)) {
                continue;
            }
            var state = widget.state();
            if (state == null) {
                skipped++;
                continue;
            }
            if (widget.savedId() == null) {
                var id = savedWidgets.insert(
                        user.id(),
                        widget.getTitle(),
                        widget.description(),
                        widget.type().name(),
                        state.sql(),
                        state.stateJson(),
                        position);
                widget.setSavedId(id);
                log.decisionRow(
                        user.id(), ActivityView.INSIGHTS, "saved widget " + id + ": " + state.sql(), "APPLIED", null);
            } else {
                savedWidgets.update(
                        widget.savedId(),
                        widget.getTitle(),
                        widget.description(),
                        state.sql(),
                        state.stateJson(),
                        position);
            }
            kept.add(widget.savedId());
            position++;
        }
        for (var id : storedIds) {
            if (!kept.contains(id)) {
                savedWidgets.delete(id);
                log.decisionRow(
                        user.id(), ActivityView.INSIGHTS, "removed widget " + id, "REJECTED", "removed by the user");
            }
        }
        storedIds.clear();
        storedIds.addAll(kept);
        dirty = false;
        saveDashboard.setEnabled(false);
        layout();
        Notification.show(
                skipped == 0
                        ? "Dashboard saved"
                        : "Dashboard saved; " + skipped + " widget(s) without a result were not kept");
    }

    private Stream<InsightWidget> widgets() {
        return dashboard.getWidgets().stream()
                .filter(InsightWidget.class::isInstance)
                .map(InsightWidget.class::cast);
    }

    private void markDirty() {
        dirty = true;
        saveDashboard.setEnabled(true);
        saveDashboard.setVisible(true);
    }
}
