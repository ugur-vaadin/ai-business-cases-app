package com.vaadin.demo.nordicsupply.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vaadin.demo.nordicsupply.domain.BulkChangeBatch;
import com.vaadin.demo.nordicsupply.domain.BulkChangeItem;

/** The batches of the supervised bulk change and their rows; items are written through their batch. */
public interface BulkChangeBatchRepository extends JpaRepository<BulkChangeBatch, Long> {

    /** The rows a batch changed, with their products, for undo. */
    @Query("select i from BulkChangeItem i join fetch i.product where i.batch.id = :batchId and i.accepted = true")
    List<BulkChangeItem> acceptedItems(@Param("batchId") Long batchId);
}
