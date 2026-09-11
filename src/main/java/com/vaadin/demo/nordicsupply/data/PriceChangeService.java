package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.demo.nordicsupply.domain.BulkChangeBatch;
import com.vaadin.demo.nordicsupply.domain.BulkChangeItem;
import com.vaadin.demo.nordicsupply.domain.PriceHistory;

/**
 * The catalogue behind the supervised bulk change, and the writes that apply or reverse one. A change is a new
 * dated price row: the row in force is closed the day before, the new one starts on the effective date, and every
 * proposal is recorded with whether the reviewer kept it.
 */
@Service
public class PriceChangeService {

    /** The catalogue is priced in euro; every price row the bulk change writes says so. */
    private static final String CURRENCY = "EUR";

    private static final String REASON = "Supplier increase";

    private static final String FIELD_NAME = "list_price";

    private final ProductRepository products;
    private final PriceHistoryRepository prices;
    private final BulkChangeBatchRepository batches;
    private final StaffRepository staff;

    public PriceChangeService(
            ProductRepository products,
            PriceHistoryRepository prices,
            BulkChangeBatchRepository batches,
            StaffRepository staff) {
        this.products = products;
        this.prices = prices;
        this.batches = batches;
        this.staff = staff;
    }

    public Page<ProductRepository.CatalogueRow> catalogue(Pageable pageable) {
        return products.catalogue(pageable);
    }

    public Optional<ProductRepository.CatalogueRow> row(int productId) {
        return products.catalogueRow(productId);
    }

    /** Closes each kept product's open price row the day before, inserts the new dated row, records the batch. */
    @Transactional
    public long apply(
            List<Proposal> kept,
            List<Proposal> rejected,
            LocalDate effectiveFrom,
            String description,
            String prompt,
            int userId) {
        var now = LocalDateTime.now();
        var batch = new BulkChangeBatch();
        batch.setCreatedAt(now);
        batch.setCreatedBy(userId);
        batch.setPrompt(prompt);
        batch.setDescription(description);
        batch.setStatus("APPLIED");
        batch.setAppliedAt(now);
        var author = staff.findById(userId).orElse(null);
        for (var proposal : kept) {
            prices.findByProductIdAndValidToIsNull(proposal.productId()).ifPresent(open -> {
                open.setValidTo(effectiveFrom.minusDays(1));
                prices.save(open);
            });
            var row = new PriceHistory();
            row.setProduct(products.getReferenceById(proposal.productId()));
            row.setListPrice(proposal.proposed());
            row.setCurrency(CURRENCY);
            row.setValidFrom(effectiveFrom);
            row.setReason(REASON);
            row.setCreatedBy(author);
            row.setCreatedAt(now);
            // the item records the id of the price row it inserted, so the row is flushed first
            var inserted = prices.saveAndFlush(row);
            batch.addItem(item(proposal, effectiveFrom, true, inserted.getId()));
        }
        for (var proposal : rejected) {
            batch.addItem(item(proposal, effectiveFrom, false, null));
        }
        // the batch and its items are written together; items have identity keys, so they insert at this flush
        return batches.saveAndFlush(batch).getId();
    }

    /** Deletes the rows the batch inserted and reopens the rows it closed. */
    @Transactional
    public void undo(long batchId) {
        for (var item : batches.acceptedItems(batchId)) {
            var priceRowId = item.getPriceHistoryId();
            if (priceRowId != null) {
                // the item stays as the audit trail of the batch, but it may not point at a row that is gone
                item.setPriceHistoryId(null);
                batches.flush();
                prices.deleteById(priceRowId);
            }
            var productId = item.getProduct().getId();
            prices.findByProductIdAndValidTo(productId, item.getEffectiveFrom().minusDays(1))
                    .ifPresent(closed -> {
                        closed.setValidTo(null);
                        prices.save(closed);
                    });
        }
        batches.findById(batchId).ifPresent(batch -> {
            batch.setStatus("UNDONE");
            batch.setUndoneAt(LocalDateTime.now());
            batches.save(batch);
        });
    }

    private BulkChangeItem item(Proposal proposal, LocalDate effectiveFrom, boolean accepted, Integer priceRowId) {
        var item = new BulkChangeItem();
        item.setProduct(products.getReferenceById(proposal.productId()));
        item.setFieldName(FIELD_NAME);
        item.setOldValue(String.valueOf(proposal.current()));
        item.setNewValue(String.valueOf(proposal.proposed()));
        item.setEffectiveFrom(effectiveFrom);
        item.setAccepted(accepted);
        item.setPriceHistoryId(priceRowId);
        return item;
    }
}
