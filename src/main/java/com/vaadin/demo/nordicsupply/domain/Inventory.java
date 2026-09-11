package com.vaadin.demo.nordicsupply.domain;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * What one warehouse holds of one product.
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    private Integer id;

    @Column(name = "on_hand")
    private Integer onHand;

    @Column(name = "reserved")
    private Integer reserved;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    @Column(name = "next_inbound_date")
    private LocalDate nextInboundDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    protected Inventory() {}

    public Integer getId() {
        return id;
    }

    public Integer getOnHand() {
        return onHand;
    }

    public Integer getReserved() {
        return reserved;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public LocalDate getNextInboundDate() {
        return nextInboundDate;
    }

    public Product getProduct() {
        return product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Inventory other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
