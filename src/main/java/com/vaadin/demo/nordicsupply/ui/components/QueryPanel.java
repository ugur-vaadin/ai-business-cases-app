package com.vaadin.demo.nordicsupply.ui.components;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;

/**
 * The "ask a question" panel of Insights: a prompt bar with a type selector and an arrow, suggested questions, and
 * the privacy footnote. It sits in the middle of an empty dashboard and, once widgets exist, inside the "New Query"
 * tile at the end of the dashboard, where it stacks its parts to fit one cell.
 */
public class QueryPanel extends Div {

    /** About five lines; a longer question scrolls inside the field instead of pushing the chips off the screen. */
    private static final String MAX_QUESTION_HEIGHT = "9rem";

    /** What the panel promises about the data: the model is shown the schema, never the rows. */
    public static final String FOOTNOTE = "Schema-only queries · No data leaves your network";

    /** Where the panel sits; a tile has less room, so it shows the short labels and stacks the bar. */
    public enum Mode {
        CENTER,
        TILE
    }

    /** What a question asks for: a widget of either kind. */
    public enum Ask {
        TABLE("Create table", "Table"),
        CHART("Create chart", "Chart");

        private final String action;
        private final String shortLabel;

        Ask(String action, String shortLabel) {
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

    /** Grows with the question up to a few lines; Enter sends, Shift+Enter starts a new line. */
    private final TextArea question = new TextArea();

    private final Select<Ask> type = new Select<>();
    private final FlexLayout tryChips = new FlexLayout();
    private final Span footnote = new Span(FOOTNOTE);

    private final Map<Ask, List<String>> chips;
    private final BiConsumer<String, Ask> onAsk;

    /**
     * @param chips the suggested questions per kind of request; a kind without an entry shows none
     * @param onAsk receives the question and the kind the user chose
     */
    public QueryPanel(Map<Ask, List<String>> chips, BiConsumer<String, Ask> onAsk) {
        this.chips = chips;
        this.onAsk = onAsk;
        addClassName("query-panel");

        question.setClearButtonVisible(true);
        question.setWidthFull();
        question.setMaxHeight(MAX_QUESTION_HEIGHT);
        question.addClassName("prompt-question");
        type.setItems(Ask.values());
        type.setValue(Ask.TABLE);
        type.addClassName("prompt-type");
        var send = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> submit(question.getValue()));
        send.addThemeVariants(ButtonVariant.PRIMARY);
        send.setAriaLabel("Ask");
        // Enter sends, Shift+Enter is a line break; the value travels with the key event so nothing is lost when the
        // field has not synced yet
        question.getElement()
                .addEventListener(
                        "keydown",
                        e -> submit(e.getEventData().path("element.value").asString("")))
                .setFilter("event.key === 'Enter' && !event.shiftKey")
                .addEventData("element.value")
                .preventDefault();
        var bar = new HorizontalLayout(question, type, send);
        bar.addClassName("prompt-bar");
        bar.setAlignItems(FlexLayout.Alignment.CENTER);
        bar.expand(question);

        tryChips.addClassName("chips");
        tryChips.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tryChips.setAlignItems(FlexLayout.Alignment.CENTER);
        type.addValueChangeListener(e -> showChips());
        showChips();
        footnote.addClassName("footnote");
        add(bar, tryChips, footnote);
        setMode(Mode.CENTER);
    }

    public void setMode(Mode mode) {
        getElement().setAttribute("mode", mode.name().toLowerCase());
        boolean full = mode == Mode.CENTER;
        question.setPlaceholder(full ? "Ask about orders, shipments, claims, products" : "Ask about live operations");
        type.setItemLabelGenerator(full ? Ask::action : Ask::shortLabel);
        type.setWidth(full ? "10rem" : null); // in a tile the selector takes the room the arrow leaves
    }

    /** The suggestions follow the kind of request: a table and a chart want different examples. */
    private void showChips() {
        tryChips.removeAll();
        var kind = type.getValue();
        var suggestions = chips.getOrDefault(kind, List.of());
        tryChips.setVisible(!suggestions.isEmpty());
        tryChips.add(new Span("Try:"));
        for (var chip : suggestions) {
            var b = new Button(chip, e -> onAsk.accept(chip, kind));
            b.addThemeVariants(ButtonVariant.SMALL);
            tryChips.add(b);
        }
    }

    private void submit(String value) {
        var text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            question.focus();
            return;
        }
        question.clear();
        onAsk.accept(text, type.getValue());
    }
}
