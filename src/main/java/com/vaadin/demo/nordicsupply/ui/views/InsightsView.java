package com.vaadin.demo.nordicsupply.ui.views;

import java.util.HashSet;
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
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
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
 * The self-service dashboard ("Insights"). An empty dashboard shows one prompt in the middle of the page. A question
 * adds a grid or chart widget and opens its chat in a popover beside it; clicking any widget does the same. The last
 * tile of the dashboard is "New Query": clicking it opens the prompt in the same popover, so a new question is always
 * one click away. The popover closes with Escape or its own close button. Widgets can be renamed, moved, resized and
 * removed; "Save Dashboard" keeps what is on screen (inserting, updating and deleting rows to match). Dashboards are
 * personal.
 * <p>
 * The dashboard's own selection (the state its move, resize and remove controls need) follows keyboard focus and is
 * cleared as soon as focus leaves the widget, which it does when the user types into the chat. The view therefore
 * keeps its own notion of the <em>active</em> widget, set by a click, and never reads the dashboard's selection.
 */
@Route(value = "insights", layout = MainLayout.class)
@PageTitle("Insights")
public class InsightsView extends VerticalLayout implements HasReadme {

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
            named and described by the assistant in business terms. Select a widget and its chat opens beside it,
            so you can refine it: "group it by week", "only the Göteborg warehouse", "add the customer". A table stays a
            table and a chart stays a chart; the **New Query** tile at the end of the dashboard makes the other kind.

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
            same query the next time you open Insights. Every question and every decision is recorded in the Activity
            log.
            """);
    private static final int CHIPS_SHOWN = 3;

    /**
     * Client-side test for a click that should open the widget's chat: one on the widget's title row, its body (the
     * chart or the grid's rows) or the dashboard's select overlay, but never one that a control handles itself: the
     * pencil, the dashboard's move, resize and remove buttons and their apply controls, a grid header (sorting), or a
     * column resize handle.
     */
    private static final String CLICK_OPENS_CHAT =
            """
            (() => {
                const path = event.composedPath();
                const part = el => (el.getAttribute && el.getAttribute('part')) || '';
                const onControl = path.some(el => el.localName === 'vaadin-button'
                        || el.localName === 'vaadin-dashboard-button'
                        || el.localName === 'vaadin-grid-sorter'
                        || /header-cell|resize/.test(part(el)));
                if (onControl) {
                    return false;
                }
                return path.some(el => el.id === 'focus-button'
                        || (el.classList && el.classList.contains('widget-body'))
                        || part(el).split(' ').includes('title')
                        || part(el).split(' ').includes('header'));
            })()""";

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
    private final Div empty = new Div();
    private final Div scroller = new Div(dashboard);
    private final DashboardWidget newQuery = new DashboardWidget();
    private final Popover popover = new Popover();
    private final Set<Integer> storedIds = new HashSet<>();
    private DashboardWidget active;
    private boolean dirty;

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
        var header = new HorizontalLayout(heading, spacer, saveDashboard);
        header.addClassName("insights-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.START);
        header.expand(spacer);

        var chips = pack.declaration().dashboardChips();
        query = new QueryPanel(chips.subList(0, Math.min(CHIPS_SHOWN, chips.size())), this::ask);

        dashboard.addClassName("insights-dashboard");
        dashboard.setWidthFull();
        dashboard.setEditable(true);
        dashboard.setMaximumColumnCount(3); // three tiles across, as designed; the popover is about one tile wide
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

        var newQueryButton = new Button("New Query", VaadinIcon.PLUS.create());
        newQueryButton.addClassName("new-query-button");
        newQuery.setContent(newQueryButton);
        newQuery.addClassName("new-query-widget");
        newQuery.getElement().addEventListener("click", e -> activate(newQuery));
        // the dashboard has no per-widget switch for moving; the tile stays put by refusing the drag and the arrow
        // keys before the widget's own handlers see them (its header, with the move button, is hidden by CSS)
        newQuery.getElement()
                .executeJs(
                        """
                        this.addEventListener('dragstart', e => { e.preventDefault(); e.stopImmediatePropagation(); }, true);
                        this.addEventListener('keydown', e => {
                            if (e.key.startsWith('Arrow') || e.key === 'Backspace' || e.key === 'Delete') {
                                e.stopImmediatePropagation();
                            }
                        }, true);
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
     * Empty: the prompt in the middle, nothing else. With widgets: the dashboard, ending with the New Query tile. Save
     * Dashboard shows while there is something to save: any widget, or the removal of the last one.
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
        } else if (!dashboard.getWidgets().contains(newQuery)) {
            dashboard.add(newQuery);
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

    /** Opens the popover beside the given tile: the widget's chat, or the prompt for the New Query tile. */
    private void activate(DashboardWidget tile) {
        if (tile == active && popover.isOpened()) {
            return;
        }
        setActive(tile);
        Div content;
        if (tile instanceof InsightWidget widget) {
            var footnote = new Span(QueryPanel.FOOTNOTE);
            footnote.addClassName("footnote");
            content = new Div();
            // the widget itself shows only its title; what it contains, in business terms, is said here
            var intro = new Span(chatIntro(widget));
            intro.addClassName("chat-intro");
            content.add(intro, widget.chat(), widget.thinkingIndicator(), footnote);
            content.addClassName("popover-chat");
        } else {
            query.setMode(QueryPanel.Mode.POPOVER);
            content = query;
        }
        var close = new Button(VaadinIcon.CLOSE.create(), e -> popover.close());
        close.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        close.addClassName("popover-close");
        close.setAriaLabel("Close");
        popover.removeAll();
        popover.add(close, content);
        if (popover.getTarget() != tile) {
            popover.setTarget(tile);
        }
        popover.open();
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

    /** Marks the active tile (gold outline, check on a widget) and clears the previous one. */
    private void setActive(DashboardWidget tile) {
        if (active != null && active != tile) {
            active.removeClassName("active");
            if (active instanceof InsightWidget w) {
                w.setSelectedMark(false);
            }
        }
        active = tile;
        if (tile != null) {
            tile.addClassName("active");
            if (tile instanceof InsightWidget w) {
                w.setSelectedMark(true);
            }
        }
    }

    /** A question adds a widget before the New Query tile, selects it, and asks it there. */
    private void ask(String question, InsightWidget.Type type) {
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
        var widget =
                new InsightWidget(type, title, new JdbcDatabaseProvider(aiJdbc, pack), providers, log, user, model);
        widget.onEdit(this::editDialog);
        widget.onChanged(w -> markDirty());
        widget.getElement()
                .addEventListener("click", e -> {
                    if (e.getEventData().path(CLICK_OPENS_CHAT).asBoolean(false)) {
                        activate(widget);
                    }
                })
                .addEventData(CLICK_OPENS_CHAT);
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

    private void markDirty() {
        dirty = true;
        saveDashboard.setEnabled(true);
        saveDashboard.setVisible(true);
    }
}
