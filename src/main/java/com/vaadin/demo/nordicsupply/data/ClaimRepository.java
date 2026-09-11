package com.vaadin.demo.nordicsupply.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vaadin.demo.nordicsupply.domain.Claim;

/** Claims: the open-claims tile and the numbering the claim form continues. */
public interface ClaimRepository extends JpaRepository<Claim, Integer> {

    @EntityGraph(attributePaths = {"customer", "order", "shipment", "assignedTo"})
    Optional<Claim> findByClaimNumber(String claimNumber);

    Optional<Claim> findFirstByOrderByIdDesc();

    @Query(
            "select count(c) from Claim c where c.status not in (com.vaadin.demo.nordicsupply.domain.ClaimStatus.RESOLVED, "
                    + "com.vaadin.demo.nordicsupply.domain.ClaimStatus.CLOSED, com.vaadin.demo.nordicsupply.domain.ClaimStatus.REJECTED)")
    long countOpen();
}
