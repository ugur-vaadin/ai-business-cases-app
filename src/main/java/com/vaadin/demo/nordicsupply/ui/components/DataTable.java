package com.vaadin.demo.nordicsupply.ui.components;

import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.vaadin.demo.nordicsupply.data.SearchTerms;
import com.vaadin.demo.nordicsupply.util.Formats;

/**
 * A read-only, lazily loaded grid over a repository query with a search box: the Orders, Customers and Products
 * views of the back office. The view says which columns to show and which repository method fetches a page; the
 * search text reaches the query as a parameter, never as SQL.
 */
public class DataTable<T> extends VerticalLayout {

    /** One grid column: its header and the value it reads from a row. */
    public record Col<T>(String header, ValueProvider<T, Object> value) {}

    private final Grid<T> grid = new Grid<>();
    private final TextField search = new TextField();

    public DataTable(List<Col<T>> columns, BiFunction<String, Pageable, Page<T>> fetch, String searchPlaceholder) {
        addClassName("data-table");
        setPadding(false);
        setSizeFull();
        search.setPlaceholder(searchPlaceholder);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        for (var column : columns) {
            grid.addColumn(row -> Formats.value(column.value().apply(row)))
                    .setHeader(column.header())
                    .setAutoWidth(true)
                    .setResizable(true);
        }
        grid.setSizeFull();
        grid.setItems(q -> fetch.apply(term(), PageRequest.of(q.getPage(), q.getPageSize())).getContent().stream(), q ->
                (int) fetch.apply(term(), PageRequest.of(0, 1)).getTotalElements());
        add(search, grid);
        expand(grid);
    }

    /** Prefills the search box, e.g. from a {@code q} query parameter in the URL. */
    public void setSearch(String text) {
        search.setValue(text == null ? "" : text);
    }

    private String term() {
        return SearchTerms.like(search.getValue());
    }
}
