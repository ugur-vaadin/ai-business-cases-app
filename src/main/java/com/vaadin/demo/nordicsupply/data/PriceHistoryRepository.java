package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.demo.nordicsupply.domain.PriceHistory;

/** The dated prices of a product: what the bulk change closes, writes and reopens. */
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {

    Optional<PriceHistory> findByProductIdAndValidToIsNull(Integer productId);

    Optional<PriceHistory> findByProductIdAndValidTo(Integer productId, LocalDate validTo);
}
