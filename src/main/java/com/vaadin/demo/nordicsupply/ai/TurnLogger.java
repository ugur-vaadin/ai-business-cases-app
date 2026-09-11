package com.vaadin.demo.nordicsupply.ai;

import java.util.function.Supplier;

import com.vaadin.flow.component.ai.orchestrator.RequestInterceptor;
import com.vaadin.flow.component.ai.orchestrator.RequestListener;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.ResponseMetadata;

import com.vaadin.demo.nordicsupply.data.ActivityLog;
import com.vaadin.demo.nordicsupply.domain.ActivityView;

/**
 * Writes every turn of one orchestrator to the activity log. The prompt as typed and the prompt as sent go in on
 * request, the answer and the token usage on response. Attach all three hooks on the orchestrator builder:
 * {@code withRequestInterceptor(logger.interceptor())}, {@code withRequestListener(logger.request())} and
 * {@code withResponseListener(logger.response())}. The interceptor sees the message before any rewriting (it is
 * the hook a masking step would use), the listener sees what actually leaves for the provider.
 */
public class TurnLogger {

    /** The name the orchestrator writes assistant turns under; the widget looks for it in the list. */
    public static final String ASSISTANT_NAME = "Assistant";

    private final ActivityLog log;
    private final ActivityView view;
    private final Supplier<Integer> userId;
    private final Supplier<Integer> widgetId;
    private final String dataScope;
    private final String model;
    // written on the UI thread (request), read on the provider's thread (response)
    private volatile long lastId = -1;
    private volatile String typed;
    private String lastPrompt = "";

    public TurnLogger(
            ActivityLog log,
            ActivityView view,
            Supplier<Integer> userId,
            Supplier<Integer> widgetId,
            String dataScope,
            String model) {
        this.log = log;
        this.view = view;
        this.userId = userId;
        this.widgetId = widgetId;
        this.dataScope = dataScope;
        this.model = model;
    }

    /** Captures the message as the user typed it, before other interceptors or the listener see it. */
    public RequestInterceptor interceptor() {
        return event -> {
            typed = event.getUserMessage();
            lastPrompt = typed;
        };
    }

    public RequestListener request() {
        return event -> {
            var sent = event.getUserMessage();
            var prompt = typed != null ? typed : sent;
            typed = null;
            lastId = log.request(userId.get(), view, widgetId.get(), prompt, sent, dataScope, model);
        };
    }

    public ResponseListener response() {
        return event -> {
            if (lastId > 0) {
                var meta = event.getMetadata();
                var usage = meta.map(ResponseMetadata::tokenUsage).orElse(null);
                log.response(
                        lastId,
                        event.getError().map(t -> "ERROR: " + t.getMessage()).orElse(event.getResponse()),
                        meta.map(ResponseMetadata::finishReason).orElse(null),
                        usage == null ? null : usage.inputTokens(),
                        usage == null ? null : usage.outputTokens());
            }
        };
    }

    public long lastId() {
        return lastId;
    }

    /** The last message the user sent through this orchestrator, as typed. */
    public String lastPrompt() {
        return lastPrompt;
    }
}
