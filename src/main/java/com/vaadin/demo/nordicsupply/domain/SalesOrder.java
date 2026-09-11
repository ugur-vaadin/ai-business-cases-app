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
 * A sales order. The class is not called {@code Order} because ORDER is a reserved word in JPQL.
 */
@Entity
@Table(name = "orders")
public class SalesOrder {

    @Id
    private Integer id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "delivery_address_id")
    private Integer deliveryAddressId;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "channel")
    private String channel;

    @Column(name = "status")
    private String status;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Column(name = "promised_ship_date")
    private LocalDate promisedShipDate;

    @Column(name = "promised_delivery_date")
    private LocalDate promisedDeliveryDate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "total_net")
    private BigDecimal totalNet;

    @Column(name = "customer_reference")
    private String customerReference;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Staff createdBy;

    protected SalesOrder() {}

    public Integer getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Integer getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public String getChannel() {
        return channel;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getRequestedDeliveryDate() {
        return requestedDeliveryDate;
    }

    public LocalDate getPromisedShipDate() {
        return promisedShipDate;
    }

    public LocalDate getPromisedDeliveryDate() {
        return promisedDeliveryDate;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getNotes() {
        return notes;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public Staff getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SalesOrder other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
