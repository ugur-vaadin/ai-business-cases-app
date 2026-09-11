package com.vaadin.demo.nordicsupply.domain;

import java.math.BigDecimal;
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
 * One line of a sales order.
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    private Integer id;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "list_price")
    private BigDecimal listPrice;

    @Column(name = "promotion_applied")
    private boolean promotionApplied;

    @Column(name = "discount_pct")
    private BigDecimal discountPct;

    @Column(name = "line_total")
    private BigDecimal lineTotal;

    @Column(name = "status")
    private String status;

    @Column(name = "backordered_qty")
    private Integer backorderedQty;

    @Column(name = "expected_restock_date")
    private LocalDate expectedRestockDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private SalesOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    protected OrderLine() {}

    public Integer getId() {
        return id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public boolean isPromotionApplied() {
        return promotionApplied;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public String getStatus() {
        return status;
    }

    public Integer getBackorderedQty() {
        return backorderedQty;
    }

    public LocalDate getExpectedRestockDate() {
        return expectedRestockDate;
    }

    public SalesOrder getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OrderLine other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
