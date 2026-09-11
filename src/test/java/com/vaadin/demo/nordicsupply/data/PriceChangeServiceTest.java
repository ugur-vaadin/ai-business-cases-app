package com.vaadin.demo.nordicsupply.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** The write side of the supervised bulk change: a dated row per kept product, and an undo that reverses it. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriceChangeServiceTest {

    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 10, 1);

    @Autowired
    private PriceChangeService service;

    @Autowired
    private PriceHistoryRepository prices;

    @Autowired
    private BulkChangeBatchRepository batches;

    @Test
    void appliesAndUndoesADatedChange() {
        var catalogue = service.catalogue(PageRequest.of(0, 2)).getContent();
        assertThat(catalogue).hasSize(2);
        var proposals = catalogue.stream()
                .map(row -> new Proposal(
                        row.id(), row.sku(), row.listPrice(), row.listPrice().add(new BigDecimal("5.00"))))
                .toList();
        var openBefore = proposals.stream()
                .map(p -> prices.findByProductIdAndValidToIsNull(p.productId()).orElseThrow())
                .toList();

        var batchId = service.apply(proposals, List.of(), EFFECTIVE_FROM, "test increase", "raise by 5", 1);

        assertThat(batches.findById(batchId)).get().extracting("status").isEqualTo("APPLIED");
        assertThat(batches.acceptedItems(batchId)).hasSize(2);
        for (var row : openBefore) {
            assertThat(prices.findById(row.getId()).orElseThrow().getValidTo()).isEqualTo(EFFECTIVE_FROM.minusDays(1));
        }
        for (var proposal : proposals) {
            var open =
                    prices.findByProductIdAndValidToIsNull(proposal.productId()).orElseThrow();
            assertThat(open.getListPrice()).isEqualByComparingTo(proposal.proposed());
            assertThat(open.getValidFrom()).isEqualTo(EFFECTIVE_FROM);
        }

        var inserted = batches.acceptedItems(batchId).stream()
                .map(i -> i.getPriceHistoryId())
                .toList();
        service.undo(batchId);

        assertThat(batches.findById(batchId)).get().extracting("status").isEqualTo("UNDONE");
        for (var id : inserted) {
            assertThat(prices.findById(id)).isEmpty();
        }
        for (var row : openBefore) {
            assertThat(prices.findById(row.getId()).orElseThrow().getValidTo()).isNull();
        }
    }
}
