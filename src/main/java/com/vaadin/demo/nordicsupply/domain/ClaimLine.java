package com.vaadin.demo.nordicsupply.domain;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One affected line of a claim.
 */
@Entity
@Table(name = "claim_lines")
public class ClaimLine {

    @Id
    private Integer id;

    @Column(name = "quantity_affected")
    private Integer quantityAffected;

    @Column(name = "pallet_number")
    private Integer palletNumber;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "issue_note")
    private String issueNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id")
    private OrderLine orderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    protected ClaimLine() {}

    public Integer getId() {
        return id;
    }

    public Integer getQuantityAffected() {
        return quantityAffected;
    }

    public Integer getPalletNumber() {
        return palletNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getIssueNote() {
        return issueNote;
    }

    public Claim getClaim() {
        return claim;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClaimLine other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
