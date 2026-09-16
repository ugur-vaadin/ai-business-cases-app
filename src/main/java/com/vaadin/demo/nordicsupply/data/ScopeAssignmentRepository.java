package com.vaadin.demo.nordicsupply.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.demo.nordicsupply.domain.ScopeAssignment;

/** Which employees can sign in, and what each of them may see. */
public interface ScopeAssignmentRepository extends JpaRepository<ScopeAssignment, Integer> {

    List<ScopeAssignment> findAllByOrderByUserId();
}
