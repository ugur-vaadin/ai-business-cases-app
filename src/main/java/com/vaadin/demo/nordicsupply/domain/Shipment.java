package com.vaadin.demo.nordicsupply.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A dispatch of one order from one warehouse.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private Integer id;

    @Column(name = "shipment_number")
    private String shipmentNumber;

    @Column(name = "delivery_address_id")
    private Integer deliveryAddressId;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "status")
    private String status;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "package_count")
    private Integer packageCount;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private SalesOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    protected Shipment() {}

    public Integer getId() {
        return id;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public Integer getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public String getStatus() {
        return status;
    }

    public Integer getPalletCount() {
        return palletCount;
    }

    public Integer getPackageCount() {
        return packageCount;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public SalesOrder getOrder() {
        return order;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Shipment other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
