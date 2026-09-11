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
 * An item of the catalogue. Its price lives in {@link PriceHistory}, one dated row at a time.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private Integer id;

    @Column(name = "sku")
    private String sku;

    @Column(name = "ean")
    private String ean;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "variant")
    private String variant;

    @Column(name = "colour")
    private String colour;

    @Column(name = "unit")
    private String unit;

    @Column(name = "case_pack")
    private Integer casePack;

    @Column(name = "min_order_qty")
    private Integer minOrderQty;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "active")
    private boolean active;

    @Column(name = "discontinued_on")
    private LocalDate discontinuedOn;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    protected Product() {}

    public Integer getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getEan() {
        return ean;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProductType() {
        return productType;
    }

    public String getVariant() {
        return variant;
    }

    public String getColour() {
        return colour;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getCasePack() {
        return casePack;
    }

    public Integer getMinOrderQty() {
        return minOrderQty;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getDiscontinuedOn() {
        return discontinuedOn;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public Category getCategory() {
        return category;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
