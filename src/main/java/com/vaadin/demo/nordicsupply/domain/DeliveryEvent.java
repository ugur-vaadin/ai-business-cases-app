package com.vaadin.demo.nordicsupply.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A tracking event of a shipment.
 */
@Entity
@Table(name = "delivery_events")
public class DeliveryEvent {

    @Id
    private Integer id;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "location")
    private String location;

    @Column(name = "note")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    protected DeliveryEvent() {}

    public Integer getId() {
        return id;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getEventType() {
        return eventType;
    }

    public String getLocation() {
        return location;
    }

    public String getNote() {
        return note;
    }

    public Shipment getShipment() {
        return shipment;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeliveryEvent other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
