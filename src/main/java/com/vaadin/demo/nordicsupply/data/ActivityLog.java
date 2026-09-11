package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.demo.nordicsupply.domain.ActivityEntry;
import com.vaadin.demo.nordicsupply.domain.ActivityView;

/**
 * The audit trail: who sent the prompt, what the model was allowed to see, what it proposed, what a person decided and
 * by which rule. Written from the AI hooks (request, response) and from the views (decisions).
 */
@Service
public class ActivityLog {

    private final ActivityEntryRepository entries;

    public ActivityLog(ActivityEntryRepository entries) {
        this.entries = entries;
    }

    /** Records a turn's request; the id comes from the table's identity column so concurrent sessions never collide. */
    @Transactional
    public long request(
            Integer userId,
            ActivityView view,
            Integer widgetId,
            String prompt,
            String promptSent,
            String dataScope,
            String model) {
        var entry = new ActivityEntry();
        entry.setOccurredAt(LocalDateTime.now());
        entry.setUserId(userId);
        entry.setView(view);
        entry.setWidgetId(widgetId);
        entry.setPrompt(prompt);
        entry.setPromptSent(promptSent);
        entry.setDataScope(dataScope);
        entry.setModelName(model);
        return entries.save(entry).getId();
    }

    @Transactional
    public void response(long id, String proposal, String finishReason, Integer inputTokens, Integer outputTokens) {
        entries.findById(id).ifPresent(entry -> {
            entry.setProposal(proposal);
            entry.setFinishReason(finishReason);
            entry.setInputTokens(inputTokens);
            entry.setOutputTokens(outputTokens);
            entries.save(entry);
        });
    }

    @Transactional
    public long decisionRow(Integer userId, ActivityView view, String proposal, String decision, String rule) {
        var entry = new ActivityEntry();
        entry.setOccurredAt(LocalDateTime.now());
        entry.setUserId(userId);
        entry.setView(view);
        entry.setProposal(proposal);
        entry.setDecision(decision);
        entry.setRejectionRule(rule);
        return entries.save(entry).getId();
    }

    public List<ActivityRow> recent(int limit) {
        return entries.recent(PageRequest.of(0, limit));
    }
}
