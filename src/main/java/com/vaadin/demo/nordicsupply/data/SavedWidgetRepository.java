package com.vaadin.demo.nordicsupply.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.demo.nordicsupply.domain.SavedWidget;

/** The saved dashboard of one user, in the order it was arranged. */
public interface SavedWidgetRepository extends JpaRepository<SavedWidget, Integer> {

    List<SavedWidget> findByUserIdOrderByPositionAscIdAsc(Integer userId);
}
