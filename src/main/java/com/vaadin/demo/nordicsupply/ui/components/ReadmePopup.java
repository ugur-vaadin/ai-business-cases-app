package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.server.VaadinSession;

/**
 * The per-view readme: a small card in the bottom-right corner that opens the full text in a centred dialog. The
 * first card of a session carries the demo's welcome text ("Explore as a Nordic Supply employee…"); after that each
 * view shows its own title and teaser. Dismissing collapses the card to a pill in the same corner, remembered per
 * view for the session, so the text stays one click away.
 */
public class ReadmePopup extends Div {

    private static final String COLLAPSED = "aicases.readme.collapsed.";
    private static final String WELCOMED = "aicases.readme.welcomed";

    private final Readme readme;
    private final Div card = new Div();
    private final Button pill;

    public ReadmePopup(Readme readme, String company) {
        this.readme = readme;
        addClassName("readme-popup");
        var session = VaadinSession.getCurrent();
        boolean welcome = !Boolean.TRUE.equals(session.getAttribute(WELCOMED));

        var title = new Span(welcome ? "Vaadin AI Demo · Explore as a " + company + " employee" : readme.title());
        title.addClassName("readme-title");
        var teaser = new Span(welcome ? "Your changes are private to this demo session." : readme.teaser());
        teaser.addClassName("readme-teaser");
        var text = new Div(title, teaser);
        text.addClassName("readme-text");
        text.addClickListener(e -> open());
        var open = new Button("Read more", e -> open());
        open.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.PRIMARY);
        var dismiss = new Button("Dismiss", e -> collapse(true));
        dismiss.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        var actions = new Div(open, dismiss);
        actions.addClassName("readme-actions");
        var icon = (welcome ? VaadinIcon.WARNING : VaadinIcon.INFO_CIRCLE).create();
        icon.addClassName("readme-icon");
        card.addClassName("readme-card");
        card.add(icon, text, actions);

        pill = new Button("Readme", VaadinIcon.QUESTION_CIRCLE.create(), e -> open());
        pill.addClassName("readme-pill");
        pill.addThemeVariants(ButtonVariant.SMALL);
        pill.setTooltipText("About this page");

        add(card, pill);
        collapse(Boolean.TRUE.equals(session.getAttribute(COLLAPSED + readme.title())));
    }

    /** Opens this view's readme in the middle of the screen. */
    public void open() {
        VaadinSession.getCurrent().setAttribute(WELCOMED, true);
        open(readme);
    }

    /** Opens any readme in the middle of the screen (the rail's Help uses it for the current view). */
    public static void open(Readme readme) {
        var dialog = new Dialog();
        dialog.addClassName("readme-dialog");
        dialog.setHeaderTitle(readme.title());
        dialog.add(new Markdown(readme.body()));
        dialog.setWidth("min(720px, 92vw)");
        dialog.setMaxHeight("85vh");
        var close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private void collapse(boolean collapsed) {
        card.setVisible(!collapsed);
        pill.setVisible(collapsed);
        var session = VaadinSession.getCurrent();
        session.setAttribute(COLLAPSED + readme.title(), collapsed);
        if (collapsed) {
            session.setAttribute(WELCOMED, true);
        }
    }
}
