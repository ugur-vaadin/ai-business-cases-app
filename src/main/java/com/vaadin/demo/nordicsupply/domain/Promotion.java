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
 * A discount on one product for a period.
 */
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "discount_pct")
    private BigDecimal discountPct;

    @Column(name = "promo_price")
    private BigDecimal promoPrice;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Staff createdBy;

    protected Promotion() {}

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public BigDecimal getPromoPrice() {
        return promoPrice;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public Product getProduct() {
        return product;
    }

    public Staff getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Promotion other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
