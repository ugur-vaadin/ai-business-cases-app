package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

/**
 * Renames a widget and says what it contains. The description is the widget's own words, kept with it and shown in
 * its chat and as its tooltip, so a saved dashboard still explains itself. A {@link Binder} on the widget does the
 * validation and the write: the title is required and trimmed, the description is free text.
 */
public class WidgetEditDialog extends Dialog {

    /** Wide enough for a widget title on one line. */
    private static final String DIALOG_WIDTH = "480px";

    private static final String TITLE_REQUIRED = "Give the widget a name";

    public WidgetEditDialog(InsightWidget widget, Runnable onApplied) {
        super("Rename or describe this widget");
        var title = new TextField("Title");
        title.setWidthFull();
        var desc = new TextArea("What it contains, in business terms");
        desc.setWidthFull();

        var binder = new Binder<InsightWidget>();
        binder.forField(title)
                .withNullRepresentation("")
                .asRequired(TITLE_REQUIRED)
                .withConverter(text -> text == null ? "" : text.trim(), String::valueOf)
                .withValidator(text -> !text.isEmpty(), TITLE_REQUIRED) // blank after trimming is empty too
                .bind(InsightWidget::getTitle, InsightWidget::setTitle);
        binder.forField(desc).bind(InsightWidget::description, InsightWidget::setDescription);
        binder.readBean(widget);

        var ok = new Button("Apply", e -> {
            if (binder.writeBeanIfValid(widget)) {
                onApplied.run();
                close();
            }
        });
        ok.addThemeVariants(ButtonVariant.PRIMARY);
        var form = new VerticalLayout(title, desc);
        form.setPadding(false);
        add(form);
        setWidth(DIALOG_WIDTH);
        getFooter().add(new Button("Cancel", e -> close()), ok);
    }
}
