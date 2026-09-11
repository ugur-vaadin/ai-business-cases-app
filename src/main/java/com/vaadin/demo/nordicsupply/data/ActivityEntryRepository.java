package com.vaadin.demo.nordicsupply.data;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vaadin.demo.nordicsupply.domain.ActivityEntry;

/** The audit trail, newest first, with the signed-in employee's name joined in. */
public interface ActivityEntryRepository extends JpaRepository<ActivityEntry, Long> {

    /**
     * A constructor projection: {@code userId} is a plain column, not an association, so a derived method cannot
     * express the join to {@code Staff}. JPQL in the annotation is the Spring Data idiom for that.
     */
    @Query(
            """
            select new com.vaadin.demo.nordicsupply.data.ActivityRow(
                a.occurredAt, s.fullName, a.view, a.prompt, a.promptSent, a.dataScope, a.proposal, a.decision,
                a.rejectionRule, a.modelName, a.inputTokens, a.outputTokens)
            from ActivityEntry a left join Staff s on s.id = a.userId
            order by a.occurredAt desc, a.id desc
            """)
    List<ActivityRow> recent(Pageable pageable);
}
