package com.vaadin.demo.nordicsupply.ui.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.data.ActivityRow;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.util.Formats;

/** The audit trail: who prompted, what the model could see, what it proposed, what a person decided. */
@Route(value = "activity", layout = MainLayout.class)
@PageTitle("Activity log")
public class ActivityLogView extends VerticalLayout implements HasReadme {

    /** A demo session never fills this; the whole log fits on the page without paging. */
    private static final int ROWS_SHOWN = 500;

    /** Prompts and answers are clipped to this in the grid; the full text is in the database. */
    private static final int CLIP_LENGTH = 160;

    private static final Readme README = new Readme(
            "Activity log",
            "Every question to the model and every decision a person made, newest first.",
            """
            ## What this page does

            This is the audit trail of the demo. Every time someone asks the assistant something, one row records
            **who** asked, on which **view**, the **prompt** as typed, the prompt as actually **sent** to the model
            provider (they differ only when a masking step rewrites it), what the model was **allowed to see**, its
            **answer or proposal**, the **model** used and the tokens it consumed.

            Decisions land here too: a widget kept or removed, a claim saved, a bulk change applied or undone, with
            the rule behind a rejection.

            ## Why it matters

            It answers the questions buyers ask first: what left our network, what did the model do with it, and who
            approved what. Nothing here is written by the model itself; the application writes it from the request
            and response hooks.
            """);

    public ActivityLogView(ActivityLog log, PackData pack) {
        setSizeFull();
        addClassName("page");
        add(new PageHeading("Activity log", "Who asked what, and what was decided"));
        var intro = new Paragraph(
                "Every AI turn and every decision, newest first. Prompt sent is what actually left for the provider.");
        intro.addClassName("intro");
        var grid = new Grid<ActivityRow>();
        grid.setSizeFull();
        grid.addColumn(r -> Formats.value(r.occurredAt())).setHeader("When").setAutoWidth(true);
        grid.addColumn(ActivityRow::who).setHeader("Who").setAutoWidth(true);
        grid.addColumn(ActivityRow::view).setHeader("View").setAutoWidth(true);
        grid.addColumn(r -> clip(r.prompt())).setHeader("Prompt").setFlexGrow(2);
        grid.addColumn(ActivityLogView::sent).setHeader("Prompt sent").setFlexGrow(1);
        grid.addColumn(r -> clip(r.dataScope())).setHeader("Model could see").setFlexGrow(1);
        grid.addColumn(r -> clip(r.proposal())).setHeader("Proposal / answer").setFlexGrow(2);
        grid.addColumn(ActivityRow::decision).setHeader("Decision").setAutoWidth(true);
        grid.addColumn(ActivityRow::rule).setHeader("Rule").setAutoWidth(true);
        grid.addColumn(ActivityRow::model).setHeader("Model").setAutoWidth(true);
        grid.addColumn(ActivityLogView::tokens).setHeader("Tokens in/out").setAutoWidth(true);
        grid.setItems(log.recent(ROWS_SHOWN));
        add(intro, grid, new ReadmePopup(README, pack.declaration().company()));
        expand(grid);
    }

    @Override
    public Readme readme() {
        return README;
    }

    /** The message that left for the provider, shown only when it differs from what the user typed. */
    private static String sent(ActivityRow r) {
        if (r.promptSent() == null) {
            return "";
        }
        return r.promptSent().equals(r.prompt()) ? "same as typed" : clip(r.promptSent());
    }

    private static String clip(Object o) {
        if (o == null) {
            return "";
        }
        var s = String.valueOf(o);
        return s.length() > CLIP_LENGTH ? s.substring(0, CLIP_LENGTH) + "…" : s;
    }

    private static String tokens(ActivityRow r) {
        var in = r.inputTokens();
        var out = r.outputTokens();
        if (in == null && out == null) {
            return "";
        }
        return tokenRepresentation(in) + " / " + tokenRepresentation(out);
    }

    private static String tokenRepresentation(Integer tokens) {
        return tokens == null ? "?" : Integer.toString(tokens);
    }
}
