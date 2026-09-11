package com.vaadin.demo.nordicsupply.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * How much of an order line went out on a shipment, and on which pallet.
 */
@Entity
@Table(name = "shipment_lines")
public class ShipmentLine {

    @Id
    private Integer id;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "pallet_number")
    private Integer palletNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id")
    private OrderLine orderLine;

    protected ShipmentLine() {}

    public Integer getId() {
        return id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPalletNumber() {
        return palletNumber;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ShipmentLine other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
