package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import com.loyalty.earning_engine.service.EarningLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarnCalculator {

    private final EarningLedgerService ledgerService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void processEarn(String memberId, Integer spendAmount, String sourceTxnId, String tier, String campaignId, String programId) {
        log.info("Processing earn for member: {}, spend: {}, tier: {}", memberId, spendAmount, tier);
        
        // 1. Calculate Base Points
        double tierMultiplier = getTierMultiplier(tier);
        int basePoints = (int) Math.floor(spendAmount * 1.0 * tierMultiplier);

        ledgerService.recordBaseEarn(memberId, basePoints, sourceTxnId);

        // Publish QP event
        java.util.Map<String, Object> qpEvent = new java.util.HashMap<>();
        qpEvent.put("memberId", memberId);
        qpEvent.put("qpAmount", basePoints);
        qpEvent.put("programId", programId);
        qpEvent.put("sourceEventId", sourceTxnId);
        kafkaTemplate.send("loyalty.earning.qp_accrued", memberId, qpEvent);
        log.info("Published QP event for member: {}, qpAmount: {}", memberId, basePoints);

        // 2. Calculate Bonus Points (if applicable)
        if (campaignId != null && !campaignId.isEmpty()) {
            double bonusMultiplier = getCampaignMultiplier(campaignId);
            if (bonusMultiplier > 1.0) {
                int bonusPoints = (int) Math.floor(basePoints * (bonusMultiplier - 1.0));
                ledgerService.recordBonusEarn(memberId, bonusPoints, sourceTxnId, campaignId);
            }
        }
    }

    private double getTierMultiplier(String tier) {
        if (tier == null) return 1.0;
        return switch (tier.toUpperCase()) {
            case "SILVER" -> 1.0;
            case "GOLD" -> 1.5;
            case "PLATINUM" -> 2.0;
            default -> 1.0;
        };
    }

    private double getCampaignMultiplier(String campaignId) {
        // Mocking campaign rules for POC
        if (campaignId.startsWith("DOUBLE")) return 2.0;
        if (campaignId.startsWith("TRIPLE")) return 3.0;
        return 1.0;
    }
}
