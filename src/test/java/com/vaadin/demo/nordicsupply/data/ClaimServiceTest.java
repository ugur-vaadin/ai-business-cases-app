package com.vaadin.demo.nordicsupply.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.demo.nordicsupply.domain.Claim;
import com.vaadin.demo.nordicsupply.domain.ClaimSource;
import com.vaadin.demo.nordicsupply.domain.ClaimStatus;
import com.vaadin.demo.nordicsupply.domain.ClaimType;
import com.vaadin.demo.nordicsupply.domain.Priority;

/** What the claim form's Save does, and the annotations the Binder validates against. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClaimServiceTest {

    @Autowired
    private ClaimService claims;

    @Autowired
    private ClaimRepository repository;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private Validator validator;

    @Test
    void createsTheNextClaimWithTheDesksOwnFields() {
        var expected = claims.nextClaimNumber();
        var number = claims.create(valid(), 1);
        assertThat(number).isEqualTo(expected);
        var saved = repository.findByClaimNumber(number);
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(saved.get().getOpenedAt())
                .isNotNull()
                .isBefore(LocalDateTime.now().plusMinutes(1));
        assertThat(saved.get().getAssignedTo()).isNotNull();
        assertThat(saved.get().getCustomer().getCustomerNumber()).isEqualTo("C-10001");
    }

    @Test
    void aClaimWithoutACustomerBreaksBeanValidation() {
        var claim = valid();
        claim.setCustomer(null);
        assertThat(validator.validate(claim))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("customer");
    }

    @Test
    void aNeededByInThePastBreaksBeanValidation() {
        var claim = valid();
        claim.setNeededBy(LocalDate.now().minusDays(1));
        assertThat(validator.validate(claim))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("neededBy");
    }

    private Claim valid() {
        var claim = new Claim();
        claim.setCustomer(customers.findByCustomerNumber("C-10001").orElseThrow());
        claim.setClaimType(ClaimType.DAMAGED);
        claim.setPriority(Priority.NORMAL);
        claim.setSource(ClaimSource.EMAIL);
        claim.setNeededBy(LocalDate.now().plusDays(7));
        claim.setDescription("Two pallets arrived crushed.");
        return claim;
    }
}
