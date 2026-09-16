package com.vaadin.demo.nordicsupply.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Which set of countries an employee may see. One row per employee who can sign in; the application reads it and
 * never writes it. In an application with real accounts this table is replaced by the roles of the authenticated
 * user.
 */
@Entity
@Table(name = "scope_assignment")
@Immutable
public class ScopeAssignment {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "role_scope")
    private String roleScope;

    protected ScopeAssignment() {}

    public Integer getUserId() {
        return userId;
    }

    /** The key of the scope, as {@code Scopes} names it. */
    public String getRoleScope() {
        return roleScope;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScopeAssignment other && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
