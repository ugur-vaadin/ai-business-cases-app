package com.vaadin.demo.nordicsupply.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vaadin.demo.nordicsupply.ai.JdbcDatabaseProvider;
import com.vaadin.demo.nordicsupply.config.AiDatabase;
import com.vaadin.demo.nordicsupply.config.ModelSettings;
import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.data.SavedWidgets;
import com.vaadin.demo.nordicsupply.domain.ActivityView;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.InsightWidget;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.QueryPanel;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.ui.components.WidgetEditDialog;

/**
 * The self-service dashboard ("Insights"). An empty dashboard shows one prompt in the middle of the page; a
 * question adds a grid or chart widget and selects it, which opens its chat in the right-hand sidebar. When nothing
 * is selected the sidebar offers a new query. The sidebar can be hidden to give the dashboard the full width; the
 * prompt then stays as a sticky footer, and selecting a widget brings the panel back. Widgets can be renamed,
 * moved, resized and removed; "Save Dashboard" keeps what is on screen (inserting, updating and deleting rows to
 * match). Dashboards are personal.
 */
@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights")
public class InsightsView extends VerticalLayout implements HasReadme {

    /** Narrower than this and a chart's axis labels collide. */
    private static final String MIN_COLUMN_WIDTH = "260px";

    /** Tall enough for a chart plus its title, so a one-by-one widget is readable. */
    private static final String ROW_HEIGHT = "360px";

    static final Readme README = new Readme(
            "Insights",
            "Ask a question about live operations; the answer becomes a widget you can keep.",
            """
            ## What this page does

            You are an analyst on the **Nordic Supply** order desk. Type a question about orders, shipments, claims or
            products, choose a table or a chart, and press the arrow. The answer appears as a widget on your dashboard,
            named and described by the assistant in business terms. Select a widget and its chat opens on the right,
            so you can refine it: "group it by week", "only the Göteborg warehouse", "add the customer". A table stays a
            table and a chart stays a chart; the New Query box makes the other kind.

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
            press **Save Dashboard**; the pencil on a widget renames or describes it. Saved widgets come back with the
            same query the next time you open Insights. The panel on the right can be hidden to give the dashboard the
            full width; the question box then stays at the bottom. Every question and every decision is recorded in the
            Activity log.
            """);
    private static final int CHIPS_SHOWN = 3;

    private final PackData pack;
    private final JdbcTemplate aiJdbc;
    private final Supplier<LLMProvider> providers;
    private final ActivityLog log;
    private final CurrentUser user;
    private final SavedWidgets savedWidgets;
    private final String model;

    private final Dashboard dashboard = new Dashboard();
    private final QueryPanel query;
    private final Button saveDashboard = new Button("Save Dashboard", e -> saveDashboard());
    private final Button panelToggle = new Button(VaadinIcon.CHEVRON_RIGHT.create(), e -> togglePanel());
    private final Div empty = new Div();
    private final Div scroller = new Div(dashboard);
    private final Div footer = new Div();
    private final Div main = new Div(scroller, footer);
    private final Div sidebar = new Div();
    private final Set<Integer> storedIds = new HashSet<>();
    private InsightWidget selected;
    private boolean panelHidden;

    public InsightsView(
            PackData pack,
            @AiDatabase JdbcTemplate aiJdbc,
            Supplier<LLMProvider> providers,
            ActivityLog log,
            CurrentUser user,
            SavedWidgets savedWidgets,
            ModelSettings ai) {
        this.pack = pack;
        this.aiJdbc = aiJdbc;
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
        panelToggle.addThemeVariants(ButtonVariant.TERTIARY);
        var header = new HorizontalLayout(heading, spacer, saveDashboard, panelToggle);
        header.addClassName("insights-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.START);
        header.expand(spacer);

        var chips = pack.declaration().dashboardChips();
        query = new QueryPanel(chips.subList(0, Math.min(CHIPS_SHOWN, chips.size())), this::ask);

        dashboard.addClassName("insights-dashboard");
        dashboard.setWidthFull();
        dashboard.setEditable(true);
        dashboard.setMaximumColumnCount(2);
        dashboard.setMinimumColumnWidth(MIN_COLUMN_WIDTH);
        dashboard.setRowHeight(ROW_HEIGHT);
        dashboard.addItemSelectedChangedListener(e -> {
            if (e.getItem() instanceof InsightWidget w) {
                if (e.isSelected()) {
                    select(w);
                } else if (w == selected) {
                    select(null);
                }
            }
        });
        dashboard.setItemRemoveHandler(e -> {
            if (e.getItem() instanceof InsightWidget w) {
                remove(w);
            }
        });
        dashboard.addItemMovedListener(e -> markDirty());
        dashboard.addItemResizedListener(e -> markDirty());

        empty.addClassName("insights-empty");
        scroller.addClassName("page-scroll");
        footer.addClassName("insights-footer");
        main.addClassName("insights-main");
        sidebar.addClassName("page-panel");
        // the sidebar is a full-height column of its own; the heading and the dashboard share the left column
        var left = new Div(header, empty, main);
        left.addClassName("page-main");
        var split = new Div(left, sidebar);
        split.addClassName("page-split");

        add(split, new ReadmePopup(README, pack.declaration().company()));
        expand(split);
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
     * Empty: the prompt in the middle, nothing else. With widgets: the dashboard, and either the sidebar (the selected
     * widget's chat, or "New Query") or, when the panel is hidden, the prompt as a sticky footer.
     */
    private void layout() {
        boolean hasWidgets = !dashboard.getWidgets().isEmpty();
        empty.setVisible(!hasWidgets);
        main.setVisible(hasWidgets);
        saveDashboard.setVisible(hasWidgets);
        panelToggle.setVisible(hasWidgets);
        sidebar.removeAll();
        footer.removeAll();
        if (!hasWidgets) {
            sidebar.setVisible(false);
            footer.setVisible(false);
            query.setMode(QueryPanel.Mode.CENTER);
            empty.removeAll();
            empty.add(query);
            return;
        }
        boolean showSidebar = !panelHidden || selected != null;
        sidebar.setVisible(showSidebar);
        footer.setVisible(!showSidebar);
        panelToggle.setIcon((showSidebar ? VaadinIcon.CHEVRON_RIGHT : VaadinIcon.CHEVRON_LEFT).create());
        panelToggle.setTooltipText(showSidebar ? "Hide the panel" : "Show the panel");
        panelToggle.setAriaLabel(showSidebar ? "Hide the panel" : "Show the panel");
        if (!showSidebar) {
            query.setMode(QueryPanel.Mode.FOOTER);
            footer.add(query);
        } else if (selected != null) {
            sidebar.add(chatBox(selected));
        } else {
            query.setMode(QueryPanel.Mode.SIDEBAR);
            sidebar.add(query);
        }
    }

    private Div chatBox(InsightWidget widget) {
        var title = new Span(widget.getTitle());
        title.addClassName("chat-title");
        var close = new Button(VaadinIcon.CLOSE.create(), e -> select(null));
        close.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        close.setAriaLabel("Close chat");
        var header = new Div(title, close);
        header.addClassName("chat-header");
        var suggestions = new Div();
        suggestions.addClassName("suggestions");
        for (var s : List.of("Group the days by week", "Add the customer", "Which data is this based on?")) {
            var b = new Button(s, e -> widget.ask(s));
            b.addThemeVariants(ButtonVariant.SMALL);
            suggestions.add(b);
        }
        var footnote = new Span(QueryPanel.FOOTNOTE);
        footnote.addClassName("footnote");
        var box = new Div(header, widget.chat(), widget.thinkingIndicator(), suggestions, footnote);
        box.addClassName("sidebar-chat");
        return box;
    }

    /** Selection is the dashboard's own (client-side) state; keep it in step when the view selects or deselects. */
    private void select(InsightWidget widget) {
        if (selected != null && selected != widget) {
            deselect();
        }
        if (widget != null) {
            widget.getElement().executeJs("this.__selected = true");
            widget.setSelectedMark(true);
            panelHidden = false; // a selected widget always shows its chat
        }
        selected = widget;
        layout();
    }

    /** Clears the selection: the widget's mark, the CSS flag its chat is shown by, and the field. */
    private void deselect() {
        if (selected != null) {
            selected.getElement().executeJs("this.__selected = false");
            selected.setSelectedMark(false);
            selected = null;
        }
    }

    private void togglePanel() {
        panelHidden = !panelHidden;
        if (panelHidden && selected != null) {
            deselect();
        }
        layout();
    }

    /** A question adds a widget titled with the question, selects it, and asks it there. */
    private void ask(String question, InsightWidget.Type type) {
        var widget = newWidget(type, question);
        dashboard.add(widget);
        markDirty();
        select(widget);
        widget.ask(question);
    }

    private InsightWidget newWidget(InsightWidget.Type type, String title) {
        var widget =
                new InsightWidget(type, title, new JdbcDatabaseProvider(aiJdbc, pack), providers, log, user, model);
        widget.onEdit(this::editDialog);
        widget.onChanged(w -> {
            markDirty();
            if (w == selected) {
                layout(); // the sidebar header shows the new title
            }
        });
        return widget;
    }

    private void editDialog(InsightWidget widget) {
        new WidgetEditDialog(widget, () -> {
                    markDirty();
                    if (widget == selected) {
                        layout();
                    }
                })
                .open();
    }

    private void remove(InsightWidget widget) {
        if (widget == selected) {
            deselect();
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
        saveDashboard.setEnabled(false);
        Notification.show(
                skipped == 0
                        ? "Dashboard saved"
                        : "Dashboard saved; " + skipped + " widget(s) without a result were not kept");
    }

    private void markDirty() {
        saveDashboard.setEnabled(true);
    }
}
