package com.loyalty.tiering_system.api;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierName;
import com.loyalty.tiering_system.domain.TierStatus;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tiering REST Controller — UC-LB-03 Apply tier upgrade (read path).
 *
 * Contract CT-12: Redemption Engine Service → Tiering System Service (Sync/HTTPS REST, GetMemberTier).
 * OpenAPI operationId: getMemberTier
 * SUT: Tiering System Service
 */
@RestController
@RequestMapping("/api/v1/tiering")
@RequiredArgsConstructor
public class MemberTierController {

    private final MemberTierRepository memberTierRepository;

    @GetMapping("/members/{memberId}/tier")
    public ResponseEntity<MemberTierResponse> getMemberTier(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "DEFAULT_PROG") String programId) {

        return memberTierRepository.findByMemberIdAndProgramId(memberId, programId)
                .map(tier -> ResponseEntity.ok(MemberTierResponse.builder()
                        .memberId(tier.getMemberId())
                        .programId(tier.getProgramId())
                        .currentTier(tier.getCurrentTier() != null ? tier.getCurrentTier().name() : TierName.SILVER.name())
                        .previousTier(tier.getPreviousTier() != null ? tier.getPreviousTier().name() : null)
                        .cumulativeQp(tier.getCumulativeQp() != null ? tier.getCumulativeQp() : 0L)
                        .status(tier.getStatus() != null ? tier.getStatus().name() : TierStatus.ACTIVE.name())
                        .effectiveFrom(tier.getEffectiveFrom())
                        .tierPeriodEnd(tier.getTierPeriodEnd())
                        .build()))
                .orElseGet(() -> ResponseEntity.ok(MemberTierResponse.builder()
                        .memberId(memberId)
                        .programId(programId)
                        .currentTier(TierName.SILVER.name())
                        .previousTier(null)
                        .cumulativeQp(0L)
                        .status(TierStatus.ACTIVE.name())
                        .effectiveFrom(LocalDateTime.now())
                        .tierPeriodEnd(LocalDate.now().plusYears(1))
                        .build()));
    }

    @Data
    @Builder
    public static class MemberTierResponse {
        private String memberId;
        private String programId;
        private String currentTier;
        private String previousTier;
        private Long cumulativeQp;
        private String status;
        private LocalDateTime effectiveFrom;
        private LocalDate tierPeriodEnd;
    }
}
