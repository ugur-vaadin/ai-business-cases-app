package com.vaadin.demo.nordicsupply.ui.views;

import java.util.List;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Sort;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.ProductRepository;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.DataTable;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;

/** The product catalogue with today's list price, read-only. */
@Route(value = "products", layout = MainLayout.class)
@PageTitle("Products")
public class ProductsView extends VerticalLayout implements HasReadme, BeforeEnterObserver {

    static final Readme README = new Readme(
            "Products",
            "The catalogue with today's wholesale list price; search by SKU, name, supplier or category.",
            """
            ## What this page does

            The catalogue: one row per product with its category, supplier and the list price valid today. Prices are
            kept as dated rows, so a price change is a new row from a date, never an edit. Search by SKU, name,
            supplier or category.
            """);

    private final DataTable<ProductRepository.Row> table;

    public ProductsView(ProductRepository products, PackData pack) {
        setSizeFull();
        setPadding(true);
        addClassName("page");
        var heading = new PageHeading("Products", "Catalogue");
        table = new DataTable<>(
                List.of(
                        new DataTable.Col<>("SKU", ProductRepository.Row::sku),
                        new DataTable.Col<>("Name", ProductRepository.Row::name),
                        new DataTable.Col<>("Category", ProductRepository.Row::category),
                        new DataTable.Col<>("Supplier", ProductRepository.Row::supplier),
                        new DataTable.Col<>("Variant", ProductRepository.Row::variant),
                        new DataTable.Col<>("List price (EUR)", ProductRepository.Row::listPrice),
                        new DataTable.Col<>("Active", ProductRepository.Row::active)),
                (term, page) -> products.search(term, page.withSort(Sort.by("sku"))),
                "Search SKU, name, supplier or category");
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
