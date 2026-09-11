package com.vaadin.demo.nordicsupply.ui.components;

import java.util.List;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * The "ask a question" panel of Insights: a prompt bar with a type selector and an arrow, suggested questions, and
 * the privacy footnote. It sits in the middle of an empty dashboard, moves to the sidebar as "New Query" once
 * widgets exist, and becomes a sticky footer under the dashboard when the sidebar is hidden.
 */
public class QueryPanel extends Div {

    /** What the panel promises about the data: the model is shown the schema, never the rows. */
    public static final String FOOTNOTE = "Schema-only queries · No data leaves your network";

    /** Where the panel sits; each place shows a different amount of it. */
    public enum Mode {
        CENTER,
        SIDEBAR,
        FOOTER
    }

    private final TextField question = new TextField();
    private final Select<InsightWidget.Type> type = new Select<>();
    private final Span title = new Span("New Query");
    private final FlexLayout tryChips = new FlexLayout(new Span("Try:"));
    private final Span footnote = new Span(FOOTNOTE);

    public QueryPanel(List<String> chips, BiConsumer<String, InsightWidget.Type> onAsk) {
        addClassName("query-panel");
        title.addClassName("query-title");

        question.setClearButtonVisible(true);
        question.setWidthFull();
        question.addClassName("prompt-question");
        type.setItems(InsightWidget.Type.values());
        type.setValue(InsightWidget.Type.GRID);
        type.addClassName("prompt-type");
        var send = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> submit(onAsk));
        send.addThemeVariants(ButtonVariant.PRIMARY);
        send.setAriaLabel("Ask");
        question.addKeyPressListener(Key.ENTER, e -> submit(onAsk));
        var bar = new HorizontalLayout(question, type, send);
        bar.addClassName("prompt-bar");
        bar.setAlignItems(FlexLayout.Alignment.CENTER);
        bar.expand(question);

        tryChips.addClassName("chips");
        tryChips.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tryChips.setAlignItems(FlexLayout.Alignment.CENTER);
        for (var chip : chips) {
            var b = new Button(chip, e -> onAsk.accept(chip, type.getValue()));
            b.addThemeVariants(ButtonVariant.SMALL);
            tryChips.add(b);
        }
        footnote.addClassName("footnote");
        add(title, bar, tryChips, footnote);
        setMode(Mode.CENTER);
    }

    public void setMode(Mode mode) {
        getElement().setAttribute("mode", mode.name().toLowerCase());
        title.setVisible(mode == Mode.SIDEBAR);
        tryChips.setVisible(mode != Mode.FOOTER);
        boolean full = mode == Mode.CENTER;
        question.setPlaceholder(full ? "Ask about orders, shipments, claims, products" : "Ask about live operations");
        type.setItemLabelGenerator(full ? InsightWidget.Type::action : InsightWidget.Type::shortLabel);
        type.setWidth(full ? "10rem" : "6.5rem");
    }

    private void submit(BiConsumer<String, InsightWidget.Type> onAsk) {
        var text = question.getValue() == null ? "" : question.getValue().trim();
        if (text.isEmpty()) {
            question.focus();
            return;
        }
        question.clear();
        onAsk.accept(text, type.getValue());
    }
}
