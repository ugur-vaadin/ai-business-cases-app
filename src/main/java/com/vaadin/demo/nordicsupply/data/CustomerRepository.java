package com.vaadin.demo.nordicsupply.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vaadin.demo.nordicsupply.domain.Customer;

/** Customers: the list view, the customers tile and the claim form's customer field. */
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    long countByActiveTrue();

    /** Rows of the Customers list. */
    record Row(
            String customerNumber,
            String name,
            String chainName,
            String segment,
            String city,
            String country,
            Integer paymentTermsDays,
            boolean active) {}

    @Query(
            """
            select new com.vaadin.demo.nordicsupply.data.CustomerRepository$Row(
                c.customerNumber, c.name, c.chainName, c.segment, c.city, c.country, c.paymentTermsDays, c.active)
            from Customer c
            where :term = '' or lower(c.name) like :term or lower(c.customerNumber) like :term
               or lower(c.city) like :term or lower(c.chainName) like :term
            """)
    Page<Row> search(@Param("term") String term, Pageable pageable);

    /** Options for the claim form's customer field. */
    @Query(
            """
            select c from Customer c
            where :term = '' or lower(c.name) like :term or lower(c.customerNumber) like :term
               or lower(c.emailDomain) like :term or lower(c.city) like :term
            order by c.name
            """)
    List<Customer> options(@Param("term") String term, Pageable pageable);
}
