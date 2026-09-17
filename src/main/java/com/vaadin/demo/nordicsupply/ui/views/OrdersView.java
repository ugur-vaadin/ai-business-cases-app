package com.vaadin.demo.nordicsupply.ui.views;

import java.util.List;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Sort;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.SalesOrderRepository;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.DataTable;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;

/** Sales orders, read-only. */
@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
public class OrdersView extends VerticalLayout implements HasReadme, BeforeEnterObserver {

    static final Readme README = new Readme(
            "Orders",
            "Every sales order, newest first; search by order number, customer or status.",
            """
            ## What this page does

            The order list every order desk has: one row per sales order with the customer, when it was placed, its
            status, the promised delivery date and the net value. Search by order number, customer name or status.

            It is deliberately ordinary. The screens answer the questions they were built for; the question nobody
            anticipated ("which orders shipped late last month, by customer?") is what **Insights** is for.
            """);

    private final DataTable<SalesOrderRepository.Row> table;

    public OrdersView(SalesOrderRepository orders, PackData pack) {
        setSizeFull();
        setPadding(true);
        addClassName("page");
        var heading = new PageHeading("Orders", "Sales orders");
        table = new DataTable<>(
                List.of(
                        new DataTable.Col<>("Order", SalesOrderRepository.Row::orderNumber),
                        new DataTable.Col<>("Customer", SalesOrderRepository.Row::customer),
                        new DataTable.Col<>("Placed", row -> row.placedAt().toLocalDate()),
                        new DataTable.Col<>("Status", SalesOrderRepository.Row::status),
                        new DataTable.Col<>("Promised delivery", SalesOrderRepository.Row::promisedDeliveryDate),
                        new DataTable.Col<>("Channel", SalesOrderRepository.Row::channel),
                        new DataTable.Col<>("Net (EUR)", SalesOrderRepository.Row::totalNet)),
                (term, page) -> orders.search(term, page.withSort(Sort.by(Sort.Direction.DESC, "placedAt", "id"))),
                "Search order number, customer or status");
        add(heading, table, new ReadmePopup(README, pack.declaration().company()));
        expand(table);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var q = event.getLocation().getQueryParameters().getParameters().get("q");
        if (q != null && !q.isEmpty()) {
            table.setSearch(q.getFirst());
        }
    }

    @Override
    public Readme readme() {
        return README;
    }
}
