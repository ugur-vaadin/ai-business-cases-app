package com.vaadin.demo.nordicsupply.data;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.demo.nordicsupply.domain.Claim;
import com.vaadin.demo.nordicsupply.domain.ClaimStatus;

/**
 * Writing the record a customer message becomes. The form supplies the fields it shows; the number, the status, the
 * time it was opened and the agent it belongs to are the desk's own conventions and are set here.
 */
@Service
public class ClaimService {

    /** The first number when the claim table is empty. */
    private static final String FIRST_NUMBER = "NEW-00001";

    private final ClaimRepository claims;
    private final StaffRepository staff;

    public ClaimService(ClaimRepository claims, StaffRepository staff) {
        this.claims = claims;
        this.staff = staff;
    }

    /** The number that follows the newest existing claim number, e.g. CL-2026-00844 → CL-2026-00845. */
    public String nextClaimNumber() {
        return claims.findFirstByOrderByIdDesc()
                .map(Claim::getClaimNumber)
                .map(ClaimNumbers::nextAfter)
                .orElse(FIRST_NUMBER);
    }

    /** Saves a new claim: number, status NEW, opened now, assigned to the signed-in user. Returns the claim number. */
    @Transactional
    public String create(Claim claim, int userId) {
        claim.setClaimNumber(nextClaimNumber());
        claim.setStatus(ClaimStatus.NEW);
        claim.setOpenedAt(LocalDateTime.now());
        claim.setAssignedTo(staff.findById(userId).orElse(null));
        return claims.save(claim).getClaimNumber();
    }
}
