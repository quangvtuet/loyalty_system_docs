package com.loyalty.tiering_system.service;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierName;
import com.loyalty.tiering_system.domain.TierStatus;
import com.loyalty.tiering_system.event.TierChangedEvent;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tier Upgrade Service — đánh giá real-time sau mỗi QP accrual.
 * FR-02-010, FR-02-011, FR-02-013: So sánh cumulative QP với threshold,
 * upgrade ngay nếu đủ (có thể vượt nhiều tier cùng lúc khi up).
 * SLA: ≤ 500ms (NFR-02-001)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TierUpgradeService {

    private static final String TIER_CHANGED_TOPIC = "loyalty.tiering.tier_changed";
    private static final String DEFAULT_PROGRAM = "DEFAULT_PROG";
    private static final int GRACE_PERIOD_DAYS = 30;

    private final MemberTierRepository memberTierRepository;
    private final KafkaTemplate<String, TierChangedEvent> kafkaTemplate;

    /**
     * Đánh giá và thực hiện upgrade tier nếu cần.
     * @param memberId      ID member
     * @param programId     ID chương trình
     * @param cumulativeQp  Tổng QP hiện tại (sau khi đã ghi accrual mới)
     */
    @Transactional
    public void evaluateAndUpgrade(String memberId, String programId, long cumulativeQp) {
        MemberTier memberTier = memberTierRepository.findByMemberIdAndProgramIdForUpdate(memberId, programId)
                .orElseGet(() -> createDefaultTier(memberId, programId));

        TierName targetTier = TierName.fromCumulativeQp(cumulativeQp);
        memberTier.setCumulativeQp(cumulativeQp);

        if (targetTier.ordinal() > memberTier.getCurrentTier().ordinal()) {
            // Upgrade warranted
            TierName previousTier = memberTier.getCurrentTier();
            memberTier.setPreviousTier(previousTier);
            memberTier.setCurrentTier(targetTier);
            memberTier.setEffectiveFrom(LocalDateTime.now());
            memberTier.setStatus(TierStatus.ACTIVE);
            memberTier.setGracePeriodEnd(null);
            memberTierRepository.save(memberTier);

            log.info("[Tier Upgrade] Member {} upgraded: {} -> {} (QP: {})", memberId, previousTier, targetTier, cumulativeQp);

            publishTierChangedEvent(memberId, programId, previousTier.name(), targetTier.name(), targetTier.getEarnMultiplier(), cumulativeQp, "UPGRADE");

        } else if (memberTier.getStatus() == TierStatus.IN_GRACE_PERIOD) {
            // Check grace rescue: nếu đang trong grace period và đã đủ threshold để giữ tier
            TierName maintenanceTier = memberTier.getCurrentTier();
            if (cumulativeQp >= maintenanceTier.getQpThreshold()) {
                memberTier.setStatus(TierStatus.ACTIVE);
                memberTier.setGracePeriodEnd(null);
                memberTierRepository.save(memberTier);
                log.info("[Grace Rescue] Member {} rescued! Tier {} maintained (QP: {})", memberId, maintenanceTier, cumulativeQp);

                publishTierChangedEvent(memberId, programId, maintenanceTier.name(), maintenanceTier.name(), maintenanceTier.getEarnMultiplier(), cumulativeQp, "GRACE_RESCUED");
            }
        } else {
            memberTierRepository.save(memberTier);
            log.debug("[Tier Eval] Member {} stays at {} (QP: {})", memberId, memberTier.getCurrentTier(), cumulativeQp);
        }
    }

    private MemberTier createDefaultTier(String memberId, String programId) {
        log.info("[Tier Init] Creating default SILVER tier for new member {}", memberId);
        MemberTier newTier = MemberTier.builder()
                .memberId(memberId)
                .programId(programId)
                .currentTier(TierName.SILVER)
                .previousTier(null)
                .effectiveFrom(LocalDateTime.now())
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .gracePeriodEnd(null)
                .status(TierStatus.ACTIVE)
                .cumulativeQp(0L)
                .build();
        return memberTierRepository.save(newTier);
    }

    private void publishTierChangedEvent(String memberId, String programId, String previousTier,
                                          String newTier, double multiplier, long qp, String reason) {
        TierChangedEvent event = TierChangedEvent.builder()
                .memberId(memberId)
                .programId(programId)
                .previousTier(previousTier)
                .newTier(newTier)
                .newEarnMultiplier(multiplier)
                .cumulativeQp(qp)
                .reason(reason)
                .build();
        kafkaTemplate.send(TIER_CHANGED_TOPIC, memberId, event);
        log.info("[Event Published] {} -> {} | reason: {} | topic: {}", previousTier, newTier, reason, TIER_CHANGED_TOPIC);
    }
}
