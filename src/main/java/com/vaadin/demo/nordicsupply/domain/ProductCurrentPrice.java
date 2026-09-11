package com.vaadin.demo.nordicsupply.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** The database view of the price in force today, one row per product. */
@Entity
@Table(name = "product_current_prices")
@Immutable
public class ProductCurrentPrice {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "list_price")
    private BigDecimal listPrice;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    protected ProductCurrentPrice() {}

    public Integer getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductCurrentPrice other && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }
}
