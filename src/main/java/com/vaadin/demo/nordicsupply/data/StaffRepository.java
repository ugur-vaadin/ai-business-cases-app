package com.vaadin.demo.nordicsupply.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.demo.nordicsupply.domain.Staff;

/** The employees, as the user selector and every {@code created_by} column need them. */
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    List<Staff> findAllByOrderById();

    Optional<Staff> findFirstByRoleOrderById(String role);
}
