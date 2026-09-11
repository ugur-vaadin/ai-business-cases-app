package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.demo.nordicsupply.domain.SavedWidget;
import com.vaadin.demo.nordicsupply.domain.WidgetType;

/**
 * The dashboard widgets a user chose to keep, in {@code saved_widgets}: title, plain-English description, type,
 * the SQL the widget runs, (for charts) the configuration that restores its look, and the position on the
 * dashboard. Dashboards are personal.
 */
@Service
public class SavedWidgets {

    /** One saved widget as stored. */
    public record Saved(int id, String title, String description, String type, String sql, String stateJson) {}

    private final SavedWidgetRepository widgets;

    public SavedWidgets(SavedWidgetRepository widgets) {
        this.widgets = widgets;
    }

    public List<Saved> forUser(int userId) {
        return widgets.findByUserIdOrderByPositionAscIdAsc(userId).stream()
                .map(w -> new Saved(
                        w.getId(),
                        w.getTitle(),
                        w.getDescription(),
                        w.getWidgetType() == null ? null : w.getWidgetType().name(),
                        w.getQuerySql(),
                        w.getStateJson()))
                .toList();
    }

    /** Stores a new widget and returns its id. */
    @Transactional
    public int insert(
            int userId, String title, String description, String type, String sql, String stateJson, int position) {
        var now = LocalDateTime.now();
        var widget = new SavedWidget();
        widget.setUserId(userId);
        widget.setTitle(title);
        widget.setDescription(description);
        widget.setWidgetType(WidgetType.valueOf(type));
        widget.setQuerySql(sql);
        widget.setStateJson(stateJson);
        widget.setPosition(position);
        widget.setCreatedAt(now);
        widget.setUpdatedAt(now);
        return widgets.save(widget).getId();
    }

    @Transactional
    public void update(int id, String title, String description, String sql, String stateJson, int position) {
        widgets.findById(id).ifPresent(widget -> {
            widget.setTitle(title);
            widget.setDescription(description);
            widget.setQuerySql(sql);
            widget.setStateJson(stateJson);
            widget.setPosition(position);
            widget.setUpdatedAt(LocalDateTime.now());
            widgets.save(widget);
        });
    }

    @Transactional
    public void delete(int id) {
        widgets.deleteById(id);
    }
}
