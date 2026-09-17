package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.Tooltip;

@Tag("poi-indicator")
public class POIIndicator extends Div {

    private Tooltip tooltip;

    public POIIndicator() {
        tooltip = Tooltip.forComponent(this);
    }

    public POIIndicator(String text) {
        this();
        setTooltipText(text);
    }

    public void setTooltipText(String text) {
        tooltip.setText(text);
    }
}
