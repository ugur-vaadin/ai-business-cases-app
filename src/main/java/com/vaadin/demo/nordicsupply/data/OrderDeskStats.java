package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * The live counts behind the tiles on the landing page: what was ordered this month, what is still open, what went
 * out late last month, and how big the catalogue and the customer base are.
 */
@Service
public class OrderDeskStats {

    /** One tile: its label, the number, and the line of context under it. */
    public record Tile(String label, int value, String detail) {}

    private final SalesOrderRepository salesOrders;
    private final ClaimRepository claims;
    private final ShipmentRepository shipments;
    private final ProductRepository products;
    private final CustomerRepository customers;

    public OrderDeskStats(
            SalesOrderRepository salesOrders,
            ClaimRepository claims,
            ShipmentRepository shipments,
            ProductRepository products,
            CustomerRepository customers) {
        this.salesOrders = salesOrders;
        this.claims = claims;
        this.shipments = shipments;
        this.products = products;
        this.customers = customers;
    }

    /** The five tiles, counted relative to the given day. */
    public List<Tile> tiles(LocalDate today) {
        var monthStart = today.withDayOfMonth(1);
        var lastMonthStart = monthStart.minusMonths(1);
        return List.of(
                new Tile(
                        "Orders this month",
                        (int) salesOrders.countByPlacedAtGreaterThanEqual(monthStart.atStartOfDay()),
                        "placed since " + monthStart),
                new Tile("Open claims", (int) claims.countOpen(), "not resolved, closed or rejected"),
                new Tile(
                        "Late dispatches last month",
                        (int) shipments.countLateDispatches(lastMonthStart.atStartOfDay(), monthStart.atStartOfDay()),
                        lastMonthStart.getMonth() + " " + lastMonthStart.getYear()),
                new Tile("Active products", (int) products.countByActiveTrue(), "in the catalogue"),
                new Tile("Customers", (int) customers.countByActiveTrue(), "active retail accounts"));
    }
}
