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

/** A person at a customer, with the marketing consent recorded for them. */
@Entity
@Table(name = "customer_contacts")
public class CustomerContact {

    @Id
    private Integer id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "role")
    private String role;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_primary")
    private boolean primaryContact;

    @Column(name = "language")
    private String language;

    @Column(name = "marketing_consent")
    private boolean marketingConsent;

    @Column(name = "consent_source")
    private String consentSource;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    protected CustomerContact() {}

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public String getConsentSource() {
        return consentSource;
    }

    public LocalDate getRetentionUntil() {
        return retentionUntil;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomerContact other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
