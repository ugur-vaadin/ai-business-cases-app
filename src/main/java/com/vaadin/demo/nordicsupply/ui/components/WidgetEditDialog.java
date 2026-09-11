package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Renames a widget and says what it contains. The description is the widget's own words, kept with it and shown
 * next to the title, so a saved dashboard still explains itself.
 */
public class WidgetEditDialog extends Dialog {

    /** Wide enough for a widget title on one line. */
    private static final String DIALOG_WIDTH = "480px";

    public WidgetEditDialog(InsightWidget widget, Runnable onApplied) {
        super("Rename or describe this widget");
        var title = new TextField("Title");
        title.setValue(widget.getTitle() == null ? "" : widget.getTitle());
        title.setRequiredIndicatorVisible(true);
        title.setWidthFull();
        var desc = new TextArea("What it contains, in business terms");
        desc.setValue(widget.description());
        desc.setWidthFull();
        var ok = new Button("Apply", e -> {
            if (title.getValue() == null || title.getValue().isBlank()) {
                title.setInvalid(true);
                title.setErrorMessage("Give the widget a name");
                title.focus();
                return;
            }
            widget.setTitle(title.getValue().trim());
            widget.setDescription(desc.getValue());
            onApplied.run();
            close();
        });
        ok.addThemeVariants(ButtonVariant.PRIMARY);
        var form = new VerticalLayout(title, desc);
        form.setPadding(false);
        add(form);
        setWidth(DIALOG_WIDTH);
        getFooter().add(new Button("Cancel", e -> close()), ok);
    }
}
