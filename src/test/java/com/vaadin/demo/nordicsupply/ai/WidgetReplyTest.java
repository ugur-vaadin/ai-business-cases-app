package com.vaadin.demo.nordicsupply.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WidgetReplyTest {

    @Test
    void readsTitleAndSummary() {
        var reply = WidgetReply.parse(
                """
                TITLE: Late shipments, August 2026
                SUMMARY: Shipments dispatched after the promised date, per week.

                Counted from late_shipments.
                """);
        assertThat(reply.title()).isEqualTo("Late shipments, August 2026");
        assertThat(reply.summary()).isEqualTo("Shipments dispatched after the promised date, per week.");
        assertThat(reply.rest()).isEqualTo("Counted from late_shipments.");
    }

    @Test
    void readsThemThroughMarkdownBold() {
        var reply = WidgetReply.parse("**TITLE:** Open claims\n**SUMMARY:** Claims not yet resolved.\n");
        assertThat(reply.title()).isEqualTo("Open claims");
        assertThat(reply.summary()).isEqualTo("Claims not yet resolved.");
    }

    @Test
    void missingPartsComeBackEmpty() {
        var reply = WidgetReply.parse("Here is the table you asked for.");
        assertThat(reply.title()).isEmpty();
        assertThat(reply.summary()).isEmpty();
        assertThat(reply.rest()).isEqualTo("Here is the table you asked for.");
    }

    @Test
    void restExcludesBothLines() {
        var reply = WidgetReply.parse("TITLE: A\nSUMMARY: B\nthe rest\nTITLE: not again\n");
        assertThat(reply.rest()).isEqualTo("the rest");
    }
}
