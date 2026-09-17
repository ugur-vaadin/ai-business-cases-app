package com.vaadin.demo.nordicsupply.ui.views;

import java.util.List;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Sort;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.CustomerRepository;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.DataTable;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;

/** The retail customers, read-only. */
@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customers")
public class CustomersView extends VerticalLayout implements HasReadme, BeforeEnterObserver {

    static final Readme README = new Readme(
            "Customers",
            "The retail companies Nordic Supply sells to; search by name, number or city.",
            """
            ## What this page does

            One row per retail company we sell to: shops, chains, online stores, rental outfitters. Search by name,
            customer number or city. Contacts and their e-mail addresses live on the application's own connection and
            are never shown to the model.
            """);

    private final DataTable<CustomerRepository.Row> table;

    public CustomersView(CustomerRepository customers, PackData pack) {
        setSizeFull();
        setPadding(true);
        addClassName("page");
        var heading = new PageHeading("Customers", "Retail accounts");
        table = new DataTable<>(
                List.of(
                        new DataTable.Col<>("Number", CustomerRepository.Row::customerNumber),
                        new DataTable.Col<>("Name", CustomerRepository.Row::name),
                        new DataTable.Col<>("Chain", CustomerRepository.Row::chainName),
                        new DataTable.Col<>("Segment", CustomerRepository.Row::segment),
                        new DataTable.Col<>("City", CustomerRepository.Row::city),
                        new DataTable.Col<>("Country", CustomerRepository.Row::country),
                        new DataTable.Col<>("Terms (days)", CustomerRepository.Row::paymentTermsDays),
                        new DataTable.Col<>("Active", CustomerRepository.Row::active)),
                (term, page) -> customers.search(term, page.withSort(Sort.by("name"))),
                "Search name, number or city");
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
