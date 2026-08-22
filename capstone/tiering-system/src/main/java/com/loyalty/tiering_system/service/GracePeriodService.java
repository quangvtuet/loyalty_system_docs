package com.loyalty.tiering_system.service;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierName;
import com.loyalty.tiering_system.domain.TierStatus;
import com.loyalty.tiering_system.event.TierChangedEvent;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Grace Period Service — xử lý trạng thái grace period và confirmed downgrade.
 *
 * FR-02-030: Apply 30-day grace period before downgrade.
 * FR-02-031: Retain benefits during grace period.
 * FR-02-033: Cancel downgrade if QP shortfall is met during grace period.
 *
 * Batch job FR-02-020: chạy vào cuối năm (31 Dec) hoặc theo cron cấu hình.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GracePeriodService {

    private static final String TIER_CHANGED_TOPIC = "loyalty.tiering.tier_changed";

    private final MemberTierRepository memberTierRepository;
    private final KafkaTemplate<String, TierChangedEvent> kafkaTemplate;

    /**
     * Khởi động grace period cho một member chưa đủ QP duy trì tier.
     * Được gọi bởi TierEvaluationJob cuối năm.
     */
    @Transactional
    public void initiateGracePeriod(MemberTier memberTier) {
        LocalDateTime gracePeriodEnd = LocalDateTime.now().plusDays(30);
        memberTier.setStatus(TierStatus.IN_GRACE_PERIOD);
        memberTier.setGracePeriodEnd(gracePeriodEnd);
        memberTierRepository.save(memberTier);

        log.info("[Grace Period] Initiated for member {} | tier: {} | grace ends: {}",
                memberTier.getMemberId(), memberTier.getCurrentTier(), gracePeriodEnd);
    }

    /**
     * Batch job quét các grace period đã hết hạn và thực hiện downgrade.
     * Chạy hàng ngày lúc 01:00 UTC để kiểm tra.
     * FR-02-023: Downgrade 1 level tại một thời điểm.
     */
    @Scheduled(cron = "0 0 1 * * *") // Every day at 01:00
    @Transactional
    public void processExpiredGracePeriods() {
        List<MemberTier> expired = memberTierRepository.findExpiredGracePeriods(
                TierStatus.IN_GRACE_PERIOD, LocalDateTime.now());

        log.info("[Grace Period Sweep] Found {} expired grace periods to process", expired.size());

        for (MemberTier memberTier : expired) {
            TierName previousTier = memberTier.getCurrentTier();
            TierName downgradedTier = getDowngradedTier(previousTier);

            memberTier.setPreviousTier(previousTier);
            memberTier.setCurrentTier(downgradedTier);
            memberTier.setStatus(TierStatus.DOWNGRADED);
            memberTier.setGracePeriodEnd(null);
            memberTierRepository.save(memberTier);

            log.info("[Downgrade] Member {} downgraded: {} -> {} (grace expired)", memberTier.getMemberId(), previousTier, downgradedTier);

            TierChangedEvent event = TierChangedEvent.builder()
                    .memberId(memberTier.getMemberId())
                    .programId(memberTier.getProgramId())
                    .previousTier(previousTier.name())
                    .newTier(downgradedTier.name())
                    .newEarnMultiplier(downgradedTier.getEarnMultiplier())
                    .cumulativeQp(memberTier.getCumulativeQp())
                    .reason("DOWNGRADED")
                    .build();
            kafkaTemplate.send(TIER_CHANGED_TOPIC, memberTier.getMemberId(), event);
        }
    }

    /**
     * Downgrade 1 bậc theo FR-02-023 (không bỏ qua tier khi xuống).
     */
    private TierName getDowngradedTier(TierName currentTier) {
        return switch (currentTier) {
            case PLATINUM -> TierName.GOLD;
            case GOLD -> TierName.SILVER;
            default -> TierName.SILVER;
        };
    }
}
