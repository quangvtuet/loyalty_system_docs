package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarnCalculator {

    private final PointTransactionRepository repository;

    @Transactional
    public void processEarn(String memberId, Integer spendAmount, String sourceTxnId, String tier, String campaignId) {
        log.info("Processing earn for member: {}, spend: {}, tier: {}", memberId, spendAmount, tier);
        
        // 1. Calculate Base Points
        double tierMultiplier = getTierMultiplier(tier);
        int basePoints = (int) Math.floor(spendAmount * 1.0 * tierMultiplier);

        PointTransaction baseTxn = PointTransaction.builder()
                .memberId(memberId)
                .type(TransactionType.EARN)
                .amount(basePoints)
                .remainingBalance(basePoints)
                .status(TransactionStatus.CONFIRMED)
                .sourceTxnId(sourceTxnId)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();
        repository.save(baseTxn);
        log.info("Saved base transaction of {} points", basePoints);

        // 2. Calculate Bonus Points (if applicable)
        if (campaignId != null && !campaignId.isEmpty()) {
            double bonusMultiplier = getCampaignMultiplier(campaignId);
            if (bonusMultiplier > 1.0) {
                int bonusPoints = (int) Math.floor(basePoints * (bonusMultiplier - 1.0));
                
                PointTransaction bonusTxn = PointTransaction.builder()
                        .memberId(memberId)
                        .type(TransactionType.BONUS)
                        .amount(bonusPoints)
                        .remainingBalance(bonusPoints)
                        .status(TransactionStatus.CONFIRMED)
                        .sourceTxnId(sourceTxnId)
                        .campaignId(campaignId)
                        .expiryDate(LocalDateTime.now().plusYears(1))
                        .build();
                repository.save(bonusTxn);
                log.info("Saved bonus transaction of {} points for campaign {}", bonusPoints, campaignId);
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
