package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * The heading every view carries: the title in the heavy italic display face, a gold subtitle under it, and the same
 * word as a large translucent watermark behind the top of the page. The view adds the {@code page} class so the
 * watermark positions against it.
 */
public class PageHeading extends Div {

    public PageHeading(String title, String subtitle) {
        addClassName("page-heading");
        var watermark = new Span(title);
        watermark.addClassName("watermark");
        watermark.getElement().setAttribute("aria-hidden", "true");
        var t = new Span(title);
        t.addClassName("page-title");
        var s = new Span(subtitle);
        s.addClassName("page-subtitle");
        add(watermark, t, s);
    }
}
