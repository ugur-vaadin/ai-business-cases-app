package com.vaadin.demo.nordicsupply.ui.views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.provider.DatabaseProviderAITools;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.JsonNode;

import com.vaadin.demo.nordicsupply.ai.JdbcDatabaseProvider;
import com.vaadin.demo.nordicsupply.ai.TurnLogger;
import com.vaadin.demo.nordicsupply.config.ModelSettings;
import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.config.ScopedConnections;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.data.PriceChangeService;
import com.vaadin.demo.nordicsupply.data.ProductRepository;
import com.vaadin.demo.nordicsupply.data.Proposal;
import com.vaadin.demo.nordicsupply.domain.ActivityView;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.session.Scopes;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.ChatPanel;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.util.Formats;

/**
 * The supervised bulk change. The catalogue manager describes the change in one sentence; the model works
 * out the rows with read-only queries and proposes a change per row through the application's own tool; nothing is
 * written until a person reviews the list, removes what looks wrong, and applies the rest. Applied changes are
 * recorded and can be undone.
 */
@Route(value = "bulk", layout = MainLayout.class)
@PageTitle("Bulk change")
public class BulkChangeView extends VerticalLayout implements HasReadme {

    static final Readme README = new Readme(
            "Bulk change",
            "Describe a price change in one sentence; review every proposed row before anything is written.",
            """
            ## What this page does

            You are the catalogue manager at **Nordic Supply**. A supplier has raised prices. Instead of exporting the
            catalogue to a spreadsheet, describe the change in the chat in one sentence, exceptions included, for
            example *"raise prices 4% on everything from Skarvind AS from the first of next month, except products
            already on promotion"*.

            The assistant works out which rows you mean with read-only queries and proposes a new price for each one.
            Nothing is written until you review the list, untick the rows that look wrong, and press **Apply kept
            rows**. What was applied is recorded and can be undone.

            ## What to look at

            * "Already on promotion" can be read two ways (on promotion today, or on the first of next month). The
              assistant says which reading it used.
            * Every row shows the current and the proposed value.
            * **Undo last applied change** reverses the last batch of this session.

            *Whether this case is part of the demo is still being decided.*
            """);

    /** The sentence the readme suggests, also shown in the intro so the reviewer can start from it. */
    private static final String EXAMPLE =
            "raise prices 4% on everything from Skarvind AS from the first of next month, except products already on promotion";

    private final PriceChangeService prices;
    private final ActivityLog log;
    private final CurrentUser user;
    private final Grid<ProductRepository.CatalogueRow> catalogue = new Grid<>();
    private final Grid<Proposal> review = new Grid<>();
    private final VerticalLayout reviewBox = new VerticalLayout();
    private final Paragraph reviewTitle = new Paragraph();
    private final List<Proposal> proposals = new ArrayList<>();
    private final Set<Object> unticked = new HashSet<>();
    private LocalDate effectiveFrom;
    private String proposalDescription = "";
    private Long lastBatch;
    private TurnLogger logged;

    public BulkChangeView(
            PackData pack,
            PriceChangeService prices,
            ScopedConnections connections,
            Supplier<LLMProvider> providers,
            ActivityLog log,
            CurrentUser user,
            ModelSettings ai) {
        this.prices = prices;
        this.log = log;
        this.user = user;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("page");
        var heading = new PageHeading("Bulk change", "Supervised price change");
        var intro = new Paragraph(
                "Describe the change in one sentence, exceptions included. The model proposes a change per row; "
                        + "nothing is written until you apply the list. Example: \"" + EXAMPLE + "\"");
        intro.addClassName("intro");

        catalogue
                .addColumn(ProductRepository.CatalogueRow::sku)
                .setHeader("SKU")
                .setAutoWidth(true);
        catalogue
                .addColumn(ProductRepository.CatalogueRow::name)
                .setHeader("Name")
                .setAutoWidth(true);
        catalogue
                .addColumn(ProductRepository.CatalogueRow::supplier)
                .setHeader("Supplier")
                .setAutoWidth(true);
        catalogue
                .addColumn(ProductRepository.CatalogueRow::category)
                .setHeader("Category")
                .setAutoWidth(true);
        catalogue
                .addColumn(row -> Formats.value(row.active()))
                .setHeader("Active")
                .setAutoWidth(true);
        catalogue
                .addColumn(row -> Formats.value(row.listPrice()))
                .setHeader("Current list price (EUR)")
                .setAutoWidth(true);
        catalogue.setSizeFull();
        catalogue.setItems(
                q -> prices.catalogue(PageRequest.of(q.getPage(), q.getPageSize())).getContent().stream(),
                q -> (int) prices.catalogue(PageRequest.of(0, 1)).getTotalElements());

        review.addColumn(Proposal::label).setHeader("Row").setFlexGrow(2);
        review.addColumn(p -> Formats.value(p.current())).setHeader("Current").setAutoWidth(true);
        review.addColumn(p -> Formats.value(p.proposed())).setHeader("Proposed").setAutoWidth(true);
        review.addComponentColumn(p -> {
                    var cb = new Checkbox(!unticked.contains(p.productId()));
                    cb.addValueChangeListener(e -> {
                        if (e.getValue()) {
                            unticked.remove(p.productId());
                        } else {
                            unticked.add(p.productId());
                        }
                    });
                    return cb;
                })
                .setHeader("Apply")
                .setAutoWidth(true);
        review.setHeight("320px");
        var apply = new Button("Apply kept rows", e -> apply());
        apply.addThemeVariants(ButtonVariant.PRIMARY);
        var discard = new Button("Discard", e -> {
            log.decisionRow(
                    user.id(), ActivityView.CATALOGUE, proposalDescription, "REJECTED", "discarded by the reviewer");
            clearProposal();
        });
        var undo = new Button("Undo last applied change", e -> undo());
        undo.addThemeVariants(ButtonVariant.TERTIARY);
        reviewBox.add(new H3("Proposed change"), reviewTitle, review, new HorizontalLayout(apply, discard, undo));
        reviewBox.setPadding(false);
        reviewBox.setVisible(false);

        var chat = new ChatPanel();
        var db = new JdbcDatabaseProvider(() -> connections.templateFor(Scopes.ALL), Optional::empty, pack);
        var controller = new ChangeController(db);
        logged = new TurnLogger(
                log,
                ActivityView.CATALOGUE,
                user::id,
                () -> null,
                "schema text + today's date; read-only queries; proposals only, no writes",
                ai.model());
        AIOrchestrator.builder(providers.get(), systemPrompt())
                .withMessageList(chat.messageList())
                .withInput(chat.messageInput())
                .withController(controller)
                .withRequestInterceptor(logged.interceptor())
                .withRequestListener(logged.request())
                .withResponseListener(logged.response())
                .withUserName(user.get().name())
                .withAssistantName(TurnLogger.ASSISTANT_NAME)
                .build();

        var left = new VerticalLayout(catalogue, reviewBox);
        left.setPadding(false);
        left.setSizeFull();
        left.expand(catalogue);
        left.addClassName("page-scroll");
        var main = new Div(heading, intro, left);
        main.addClassName("page-main");
        var panelTitle = new Span("Describe the change");
        panelTitle.addClassName("panel-title");
        var panel = new Div(panelTitle, chat);
        panel.addClassName("page-panel");
        var split = new Div(main, panel);
        split.addClassName("page-split");
        add(split, new ReadmePopup(README, pack.declaration().company()));
        expand(split);
    }

    @Override
    public Readme readme() {
        return README;
    }

    private String systemPrompt() {
        return """
                You prepare a supervised bulk change on the products table. Work in two steps and never write to the database.
                1. Find the rows the user means with read-only SQL through the query tool (read the schema first). The current
                   value of each row is product_current_prices.list_price (join on product_current_prices.product_id = products.id). Apply every exception the user states; when a phrase such as
                   "already on promotion" can be read in more than one way, say which reading you used.
                2. Call propose_change once with every affected row id, its new value, the effective date and a one-sentence
                   description of the rule you applied. Round money to two decimals.
                Then tell the user how many rows you proposed and what to check. A person reviews and applies the list.
                """;
    }

    /** The application's own tool: the model proposes; the view shows the list; a person decides. */
    private class ChangeController implements AIController {
        private final JdbcDatabaseProvider db;

        ChangeController(JdbcDatabaseProvider db) {
            this.db = db;
        }

        @Override
        public List<LLMProvider.ToolSpec> getTools() {
            var tools = new ArrayList<>(DatabaseProviderAITools.createAll(db));
            tools.add(new LLMProvider.ToolSpec() {
                @Override
                public String getName() {
                    return "propose_change";
                }

                @Override
                public String getDescription() {
                    return "Propose a new value for each listed row of products, effective from a date. Nothing is written; "
                            + "a person reviews the list. Call once with all rows.";
                }

                @Override
                public String getParametersSchema() {
                    return """
                            {"type":"object","properties":{
                              "items":{"type":"array","items":{"type":"object","properties":{
                                 "id":{"type":"integer","description":"row id"},
                                 "new_value":{"type":"number","description":"the proposed value"}},"required":["id","new_value"]}},
                              "effective_from":{"type":"string","description":"ISO date the change takes effect"},
                              "description":{"type":"string","description":"the rule applied, in one sentence, including the reading of any ambiguous phrase"}
                            },"required":["items","effective_from","description"]}
                            """;
                }

                @Override
                public String execute(JsonNode args) {
                    if (!args.has("items") || args.get("items").isEmpty()) {
                        throw new ToolException("items must contain at least one row");
                    }
                    LocalDate from;
                    try {
                        from = LocalDate.parse(args.get("effective_from").asString());
                    } catch (Exception e) {
                        throw new ToolException("effective_from must be an ISO date (YYYY-MM-DD)");
                    }
                    var list = new ArrayList<Proposal>();
                    for (var it : args.get("items")) {
                        var id = it.get("id").asInt();
                        var row = prices.row(id)
                                .orElseThrow(() -> new ToolException("no row with id " + id + " in products"));
                        list.add(new Proposal(
                                row.id(),
                                row.sku() + " · " + row.name() + " · " + row.supplier() + " · " + row.category() + " · "
                                        + row.active(),
                                row.listPrice(),
                                new BigDecimal(it.get("new_value").asString()).setScale(2, RoundingMode.HALF_UP)));
                    }
                    var desc = args.get("description").asString();
                    var ui = UI.getCurrent();
                    Runnable show = () -> showProposal(list, from, desc);
                    if (ui != null) {
                        ui.access(show::run);
                    } else {
                        getUI().ifPresent(u -> u.access(show::run));
                    }
                    return "Proposed " + list.size() + " changes effective " + from + "; the reviewer sees them now.";
                }
            });
            return tools;
        }
    }

    private void showProposal(List<Proposal> list, LocalDate from, String desc) {
        proposals.clear();
        proposals.addAll(list);
        unticked.clear();
        effectiveFrom = from;
        proposalDescription = desc;
        reviewTitle.setText(list.size() + " rows, effective " + from + ". " + desc);
        review.setItems(proposals);
        reviewBox.setVisible(true);
        if (logged != null && logged.lastId() > 0) {
            log.response(
                    logged.lastId(),
                    "proposal: " + list.size() + " rows effective " + from + " — " + desc,
                    null,
                    null,
                    null);
        }
    }

    private void clearProposal() {
        proposals.clear();
        unticked.clear();
        review.setItems(proposals);
        reviewBox.setVisible(false);
    }

    /** Hands the kept rows to the service, then records the decision and refreshes the catalogue. */
    private void apply() {
        var kept = proposals.stream()
                .filter(p -> !unticked.contains(p.productId()))
                .toList();
        if (kept.isEmpty()) {
            Notification.show("Nothing kept");
            return;
        }
        var rejected =
                proposals.stream().filter(p -> unticked.contains(p.productId())).toList();
        var batchId = prices.apply(kept, rejected, effectiveFrom, proposalDescription, logged.lastPrompt(), user.id());
        lastBatch = batchId;
        log.decisionRow(
                user.id(),
                ActivityView.CATALOGUE,
                "batch " + batchId + ": " + kept.size() + " of " + proposals.size() + " rows applied — "
                        + proposalDescription,
                kept.size() == proposals.size() ? "APPLIED" : "PARTIAL",
                kept.size() == proposals.size() ? null : "rows removed by the reviewer");
        Notification.show("Applied " + kept.size() + " changes (batch " + batchId + ")");
        clearProposal();
        catalogue.getDataProvider().refreshAll();
    }

    /** Reverses the batch applied in this session, if any. */
    private void undo() {
        if (lastBatch == null) {
            Notification.show("Nothing to undo in this session");
            return;
        }
        prices.undo(lastBatch);
        log.decisionRow(user.id(), ActivityView.CATALOGUE, "batch " + lastBatch + " undone", "UNDONE", null);
        Notification.show("Undid batch " + lastBatch);
        lastBatch = null;
        catalogue.getDataProvider().refreshAll();
    }
}
