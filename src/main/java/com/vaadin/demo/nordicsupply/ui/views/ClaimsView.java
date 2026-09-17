package com.vaadin.demo.nordicsupply.ui.views;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.form.ValueOptions;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;

import com.vaadin.demo.nordicsupply.ai.TurnLogger;
import com.vaadin.demo.nordicsupply.config.ModelSettings;
import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.data.ClaimService;
import com.vaadin.demo.nordicsupply.data.CustomerRepository;
import com.vaadin.demo.nordicsupply.data.SalesOrderRepository;
import com.vaadin.demo.nordicsupply.data.SearchTerms;
import com.vaadin.demo.nordicsupply.data.ShipmentRepository;
import com.vaadin.demo.nordicsupply.domain.ActivityView;
import com.vaadin.demo.nordicsupply.domain.Claim;
import com.vaadin.demo.nordicsupply.domain.ClaimSource;
import com.vaadin.demo.nordicsupply.domain.ClaimType;
import com.vaadin.demo.nordicsupply.domain.Customer;
import com.vaadin.demo.nordicsupply.domain.Priority;
import com.vaadin.demo.nordicsupply.domain.RequestedResolution;
import com.vaadin.demo.nordicsupply.domain.SalesOrder;
import com.vaadin.demo.nordicsupply.domain.Shipment;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.ChatPanel;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;

/**
 * Message to claim. The claim form is a bean-validated {@link Claim}: the lookups are combo boxes over the
 * live repositories (the model searches the same option lists through {@code fieldValueOptions}), and the claim type
 * reveals the fields that apply to it. The agent drops a customer message into the chat; the model fills the form;
 * the agent checks and saves, and the form's own rules reject what does not fit.
 */
@Route(value = "claims", layout = MainLayout.class)
@PageTitle("Claims")
public class ClaimsView extends VerticalLayout implements HasReadme {

    private static final Logger LOG = LoggerFactory.getLogger(ClaimsView.class);

    /** One page of lookup rows for the model; more than this is noise in a tool result. */
    private static final int MAX_OPTIONS_FOR_MODEL = 200;

    /** What a text area accepts when the column declares no length of its own. */
    private static final int DESCRIPTION_MAX_LENGTH = 1000;

    /** The name of the form field for the date a customer needs an answer by. */
    private static final String NEEDED_BY = "neededBy";

    /** The name of the form field for the amount a customer claims. */
    private static final String CLAIMED_AMOUNT = "claimedAmount";

    /** Which fields each claim type asks for; a field named by no type is always visible. */
    private static final Map<ClaimType, Set<String>> SECTIONS = Map.of(
            ClaimType.DAMAGED, Set.of(NEEDED_BY, CLAIMED_AMOUNT),
            ClaimType.MISSING_ITEMS, Set.of(NEEDED_BY, CLAIMED_AMOUNT),
            ClaimType.WRONG_ITEM, Set.of(NEEDED_BY),
            ClaimType.LATE_DELIVERY, Set.of(CLAIMED_AMOUNT),
            ClaimType.QUALITY_DEFECT, Set.of(CLAIMED_AMOUNT),
            ClaimType.PRICING_DISPUTE, Set.of(CLAIMED_AMOUNT),
            ClaimType.RETURN_REQUEST, Set.of());

    static final Readme README = new Readme(
            "Claims",
            "Drop a customer's message into the chat; the assistant fills the claim form from the live records.",
            """
            ## What this page does

            You are a support agent on the **Nordic Supply** order desk. A customer has written about a problem with a
            delivery. Pick one of the demo messages (or paste your own into the chat) and press **Fill the form from
            this message**. The assistant reads the message, finds the customer, the order and the shipment in the
            live lookups, sets the claim type, and fills the fields that apply to that type.

            ## What to look at

            * Filled fields are marked, so you can see what the assistant wrote and what was already there.
            * The claim type reveals the sections that apply and hides the rest.
            * Values that break the form's rules never reach you: the form rejects them and the model corrects them.
            * Anything still wrong you change like any other value, then **Save**.

            ## What the model sees

            The customer's message and the form's field names, labels and current values. Hidden sections stay
            hidden. Every turn is recorded in the Activity log.

            *This view is the next case to be built to the designs; the text above describes the intended flow.*
            """);

    private final ClaimService claimService;
    private final ActivityLog log;
    private final CurrentUser user;
    private final BeanValidationBinder<Claim> binder = new BeanValidationBinder<>(Claim.class);

    private final ComboBox<Customer> customer = new ComboBox<>("Customer");
    private final ComboBox<SalesOrder> order = new ComboBox<>("Order");
    private final ComboBox<Shipment> shipment = new ComboBox<>("Shipment");
    private final Select<ClaimType> claimType = new Select<>();
    private final DatePicker neededBy = new DatePicker("Needed by");
    private final BigDecimalField claimedAmount = new BigDecimalField("Claimed amount");

    public ClaimsView(
            PackData pack,
            ClaimService claimService,
            CustomerRepository customers,
            SalesOrderRepository orders,
            ShipmentRepository shipments,
            Supplier<LLMProvider> providers,
            ActivityLog log,
            CurrentUser user,
            ModelSettings ai) {
        this.claimService = claimService;
        this.log = log;
        this.user = user;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("page");
        var heading = new PageHeading("Claims", "Message to claim");

        customer.setItems(query ->
                customers.options(SearchTerms.like(query.getFilter().orElse("")), toSpringPageRequest(query)).stream());
        customer.setItemLabelGenerator(
                c -> c.getName() + " · " + c.getCustomerNumber() + ", " + c.getCity() + ", " + c.getCountry());
        customer.setClearButtonVisible(true);
        order.setItems(query -> orders
                .options(
                        customer.getValue() == null ? null : customer.getValue().getId(),
                        SearchTerms.like(query.getFilter().orElse("")),
                        toSpringPageRequest(query))
                .stream());
        order.setItemLabelGenerator(o -> o.getOrderNumber() + " · "
                + o.getPlacedAt().toLocalDate() + ", " + o.getPromisedDeliveryDate() + ", " + o.getStatus());
        order.setClearButtonVisible(true);
        shipment.setItems(query -> shipments
                .options(
                        order.getValue() == null ? null : order.getValue().getId(),
                        SearchTerms.like(query.getFilter().orElse("")),
                        toSpringPageRequest(query))
                .stream());
        shipment.setItemLabelGenerator(s -> s.getShipmentNumber() + " · " + s.getCarrier() + ", pallets "
                + s.getPalletCount()
                + (s.getDeliveredAt() == null
                        ? ""
                        : ", delivered " + s.getDeliveredAt().toLocalDate()));
        shipment.setClearButtonVisible(true);
        // a parent lookup narrows its child's options
        customer.addValueChangeListener(e -> order.getDataProvider().refreshAll());
        order.addValueChangeListener(e -> shipment.getDataProvider().refreshAll());

        claimType.setLabel("Claim type");
        claimType.setItems(ClaimType.values());
        var priority = new Select<Priority>();
        priority.setLabel("Priority");
        priority.setItems(Priority.values());
        var source = new Select<ClaimSource>();
        source.setLabel("Source");
        source.setItems(ClaimSource.values());
        var requestedResolution = new Select<RequestedResolution>();
        requestedResolution.setLabel("Requested resolution");
        requestedResolution.setItems(RequestedResolution.values());
        requestedResolution.setEmptySelectionAllowed(true);
        var description = new TextArea("Description");
        description.setMaxLength(DESCRIPTION_MAX_LENGTH);

        var form = new FormLayout();
        // auto-responsive: the column count follows the width, no breakpoints to maintain; fields flow into rows
        // on their own, the two columns share the width, and each field fills its column
        form.setAutoResponsive(true);
        form.setAutoRows(true);
        form.setMaxColumns(2);
        form.setExpandColumns(true);
        form.setExpandFields(true);
        form.add(
                customer,
                order,
                shipment,
                claimType,
                priority,
                source,
                neededBy,
                requestedResolution,
                claimedAmount,
                description);
        binder.bindInstanceFields(this);
        binder.readBean(new Claim());
        claimType.addValueChangeListener(e -> applySections());
        applySections();

        var controller = new FormAIController(form, binder);
        controller.describeField(customer, "the customer who reported the problem (required)");
        controller.describeField(order, "the order the message refers to");
        controller.describeField(shipment, "the shipment the message refers to");
        controller.describeField(
                claimType,
                "DAMAGED, MISSING_ITEMS, WRONG_ITEM, LATE_DELIVERY, QUALITY_DEFECT, PRICING_DISPUTE, RETURN_REQUEST (required)");
        controller.describeField(priority, "LOW, NORMAL, HIGH, URGENT (required)");
        controller.describeField(source, "EMAIL, PORTAL, PHONE (required)");
        controller.describeField(neededBy, "date the customer needs a resolution by");
        controller.describeField(requestedResolution, "REPLACEMENT, REDELIVERY, CREDIT, REPAIR, RETURN_AUTHORISATION");
        controller.describeField(claimedAmount, "EUR");
        controller.describeField(description, "the agent's summary");
        controller.fieldValueOptions(ValueOptions.forField(customer)
                .options((search, limit) -> customers.options(
                        SearchTerms.like(search), PageRequest.of(0, Math.min(limit, MAX_OPTIONS_FOR_MODEL))))
                .itemLabelGenerator(customer.getItemLabelGenerator()));
        controller.fieldValueOptions(ValueOptions.forField(order)
                .options((search, limit) -> orders.options(
                        customer.getValue() == null ? null : customer.getValue().getId(),
                        SearchTerms.like(search),
                        PageRequest.of(0, Math.min(limit, MAX_OPTIONS_FOR_MODEL))))
                .itemLabelGenerator(order.getItemLabelGenerator()));
        controller.fieldValueOptions(ValueOptions.forField(shipment)
                .options((search, limit) -> shipments.options(
                        order.getValue() == null ? null : order.getValue().getId(),
                        SearchTerms.like(search),
                        PageRequest.of(0, Math.min(limit, MAX_OPTIONS_FOR_MODEL))))
                .itemLabelGenerator(shipment.getItemLabelGenerator()));

        var save = new Button("Save claim", e -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        var clear = new Button("Clear", e -> binder.readBean(new Claim()));

        var chat = new ChatPanel();
        var scope =
                "the customer's message and the form's field names, labels and current values (fields hidden by the section stay hidden)";
        var logged = new TurnLogger(log, ActivityView.CLAIM_FORM, user::id, () -> null, scope, ai.model());
        var orchestrator = AIOrchestrator.builder(providers.get(), systemPrompt())
                .withMessageList(chat.messageList())
                .withInput(chat.messageInput())
                .withController(controller)
                .withRequestInterceptor(logged.interceptor())
                .withRequestListener(logged.request())
                .withResponseListener(logged.response())
                .withUserName(user.get().name())
                .withAssistantName(TurnLogger.ASSISTANT_NAME)
                .build();

        var docs = new Select<Resource>();
        docs.setLabel("Demo messages");
        docs.setItems(pack.documents());
        docs.setItemLabelGenerator(r -> String.valueOf(r.getFilename()));
        docs.setWidthFull();
        var preview = new TextArea("Message");
        preview.setWidthFull();
        preview.setMinHeight("180px");
        preview.addClassName("message-preview"); // a long message scrolls inside; the chat under it keeps its place
        preview.setReadOnly(true);
        docs.addValueChangeListener(e -> preview.setValue(e.getValue() == null ? "" : text(e.getValue())));
        var send = new Button("Fill the form from this message", e -> {
            if (!preview.getValue().isBlank()) {
                orchestrator.prompt(
                        "Create the record from this message. Search the option lists for the right records; "
                                + "leave a field empty rather than guessing.\n\n" + preview.getValue());
            }
        });
        send.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);

        var intro = new Paragraph(
                "Pick a demo message on the right or paste your own into the chat. The model finds the records in the live lookups, "
                        + "sets the type, fills the sections that apply, and the form's validation rejects what does not fit. Filled fields are marked.");
        intro.addClassName("intro");
        var formColumn = new VerticalLayout(form, new HorizontalLayout(save, clear));
        formColumn.setPadding(false);
        formColumn.setWidth(null); // the stylesheet sizes it: it reaches past the form to make room for the markers
        formColumn.addClassNames("page-scroll", "ai-form");
        var main = new Div(heading, intro, formColumn);
        main.addClassName("page-main");
        // the message and its chat live in the panel on the right, full height, as on Insights
        var panelTitle = new Span("Customer message");
        panelTitle.addClassName("panel-title");
        var panel = new Div(panelTitle, docs, preview, send, chat);
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
                You turn a customer's message into a claim record by filling the form with the form tools. Use the option lists for
                every lookup field and pick the record the message points at (dates, numbers, names in the text); never invent
                identifiers. Set the type or category field first; it decides which fields apply. If a value is rejected by the form,
                correct it in the same turn. Leave a field empty when the message does not say. Finish with two sentences: what you
                filled and what the agent should double-check.
                """;
    }

    /** Fields listed in any section are visible only when the claim type asks for them. */
    private void applySections() {
        var sectioned = SECTIONS.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        // no type chosen yet: an immutable Map rejects a null key, and nothing is revealed anyway
        var shown = claimType.getValue() == null ? Set.<String>of() : SECTIONS.get(claimType.getValue());
        neededBy.setVisible(!sectioned.contains(NEEDED_BY) || shown.contains(NEEDED_BY));
        claimedAmount.setVisible(!sectioned.contains(CLAIMED_AMOUNT) || shown.contains(CLAIMED_AMOUNT));
    }

    private void save() {
        var claim = new Claim();
        try {
            binder.writeBean(claim);
            var number = claimService.create(claim, user.id());
            log.decisionRow(user.id(), ActivityView.CLAIM_FORM, "created claims " + number, "APPLIED", null);
            Notification.show("Saved " + number);
            binder.readBean(new Claim());
        } catch (ValidationException e) {
            Notification.show("Please fix the highlighted fields");
            var messages = e.getValidationErrors().stream()
                    .map(ValidationResult::getErrorMessage)
                    .collect(Collectors.joining(", "));
            var draft = new Claim();
            binder.writeBeanAsDraft(draft, true);
            log.decisionRow(user.id(), ActivityView.CLAIM_FORM, entered(draft), "REJECTED", messages);
        }
    }

    /** What the agent (or the model) had put in the form, for the activity log. */
    private static String entered(Claim draft) {
        return String.join(
                ", ",
                "customer=" + name(draft.getCustomer()),
                "order=" + (draft.getOrder() == null ? "" : draft.getOrder().getOrderNumber()),
                "shipment="
                        + (draft.getShipment() == null
                                ? ""
                                : draft.getShipment().getShipmentNumber()),
                "claimType=" + draft.getClaimType(),
                "priority=" + draft.getPriority(),
                "source=" + draft.getSource(),
                "neededBy=" + draft.getNeededBy(),
                "requestedResolution=" + draft.getRequestedResolution(),
                "claimedAmount=" + draft.getClaimedAmount());
    }

    private static String name(Customer customer) {
        return customer == null ? "" : customer.getName();
    }

    private static String text(Resource r) {
        try {
            return r.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("document {} could not be read", r.getFilename(), e);
            return "";
        }
    }
}
