package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDateTime;

import com.vaadin.demo.nordicsupply.domain.ActivityView;

/** One line of the activity log as the view shows it: the entry joined with the employee's name. */
public record ActivityRow(
        LocalDateTime occurredAt,
        String who,
        ActivityView view,
        String prompt,
        String promptSent,
        String dataScope,
        String proposal,
        String decision,
        String rule,
        String model,
        Integer inputTokens,
        Integer outputTokens) {}
