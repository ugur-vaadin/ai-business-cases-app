package com.vaadin.demo.nordicsupply.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.demo.nordicsupply.data.ActivityEntryRepository;
import com.vaadin.demo.nordicsupply.data.BulkChangeBatchRepository;
import com.vaadin.demo.nordicsupply.data.ClaimRepository;
import com.vaadin.demo.nordicsupply.data.CustomerRepository;
import com.vaadin.demo.nordicsupply.data.PriceHistoryRepository;
import com.vaadin.demo.nordicsupply.data.ProductRepository;
import com.vaadin.demo.nordicsupply.data.SalesOrderRepository;
import com.vaadin.demo.nordicsupply.data.SavedWidgetRepository;
import com.vaadin.demo.nordicsupply.data.ShipmentRepository;
import com.vaadin.demo.nordicsupply.data.StaffRepository;

/**
 * Reads one row through every repository. Hibernate maps the pack's schema with {@code ddl-auto=none}, so nothing
 * validates the mappings at start-up; this test does, by selecting every column of every entity.
 */
@SpringBootTest
@ActiveProfiles("test")
class EntityMappingTest {

    @Autowired
    private StaffRepository staff;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private SalesOrderRepository orders;

    @Autowired
    private ShipmentRepository shipments;

    @Autowired
    private ClaimRepository claims;

    @Autowired
    private ProductRepository products;

    @Autowired
    private PriceHistoryRepository prices;

    @Autowired
    private SavedWidgetRepository savedWidgets;

    @Autowired
    private ActivityEntryRepository activity;

    @Autowired
    private BulkChangeBatchRepository batches;

    @Autowired
    private EntityManager entityManager;

    @Test
    void everySeededTableReadsOneRow() {
        Map<String, PagingAndSortingRepository<?, ?>> seeded = new LinkedHashMap<>();
        seeded.put("Staff", staff);
        seeded.put("Customer", customers);
        seeded.put("SalesOrder", orders);
        seeded.put("Shipment", shipments);
        seeded.put("Claim", claims);
        seeded.put("Product", products);
        seeded.put("PriceHistory", prices);
        seeded.put("SavedWidget", savedWidgets);
        seeded.forEach((name, repository) -> assertThat(
                        repository.findAll(PageRequest.of(0, 1)).getContent())
                .as(name)
                .hasSize(1));
    }

    /**
     * Children of an aggregate (lines, contacts, events, credit notes), stock, promotions and the lookup tables have no
     * repository of their own; their mappings are exercised through JPQL here and through the parent repositories in the cards.
     */
    @Test
    void theChildAndLookupTablesReadOneRow() {
        for (var entity : List.of(
                "Category",
                "Supplier",
                "Warehouse",
                "CustomerContact",
                "OrderLine",
                "ShipmentLine",
                "DeliveryEvent",
                "ClaimLine",
                "CreditNote",
                "ProductCurrentPrice",
                "Promotion",
                "Inventory")) {
            assertThat(one("select e from " + entity + " e")).as(entity).hasSize(1);
        }
    }

    @Test
    void theApplicationsOwnTablesRead() {
        assertThat(activity.findAll(PageRequest.of(0, 1))).isNotNull();
        assertThat(batches.findAll(PageRequest.of(0, 1))).isNotNull();
        assertThat(batches.acceptedItems(-1L)).isEmpty();
    }

    private List<?> one(String jpql) {
        return entityManager.createQuery(jpql).setMaxResults(1).getResultList();
    }
}
