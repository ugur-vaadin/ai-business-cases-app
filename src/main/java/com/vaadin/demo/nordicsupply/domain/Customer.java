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
 * A retail customer of the wholesaler.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private Integer id;

    @Column(name = "customer_number")
    private String customerNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "chain_name")
    private String chainName;

    @Column(name = "segment")
    private String segment;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "street")
    private String street;

    @Column(name = "vat_number")
    private String vatNumber;

    @Column(name = "email_domain")
    private String emailDomain;

    @Column(name = "phone")
    private String phone;

    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "customer_discount_pct")
    private BigDecimal customerDiscountPct;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "active")
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_id")
    private Staff accountManager;

    protected Customer() {}

    public Integer getId() {
        return id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getName() {
        return name;
    }

    public String getChainName() {
        return chainName;
    }

    public String getSegment() {
        return segment;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public Integer getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public BigDecimal getCustomerDiscountPct() {
        return customerDiscountPct;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public Staff getAccountManager() {
        return accountManager;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
