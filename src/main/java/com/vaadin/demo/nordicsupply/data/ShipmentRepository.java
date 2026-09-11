package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vaadin.demo.nordicsupply.domain.Shipment;

/** Shipments: the late-dispatch tile and the claim form's shipment field. */
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    /** Shipments dispatched after the order's promised ship date, shipped within [from, to). */
    @Query(
            "select count(s) from Shipment s join s.order o "
                    + "where s.shippedAt >= :from and s.shippedAt < :to and cast(s.shippedAt as LocalDate) > o.promisedShipDate")
    long countLateDispatches(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Options for the claim form's shipment field, narrowed to one order when given. */
    @Query(
            """
            select s from Shipment s
            where (:orderId is null or s.order.id = :orderId)
              and (:term = '' or lower(s.shipmentNumber) like :term or lower(s.trackingNumber) like :term)
            order by s.shippedAt desc
            """)
    List<Shipment> options(@Param("orderId") Integer orderId, @Param("term") String term, Pageable pageable);
}
