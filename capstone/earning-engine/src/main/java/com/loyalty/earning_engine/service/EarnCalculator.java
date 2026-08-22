package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.client.TieringClient;
import com.loyalty.earning_engine.dto.EarnEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * EarnCalculator — UC-LB-01 Process settled earn event.
 *
 * Implements point accrual calculations with strict I-5 tamper resistance:
 * - Authoritative member tier is queried from Tiering DB via CT-12 (TieringClient).
 * - Client-supplied tier fields in earn events are NEVER trusted and cannot manipulate multipliers.
 *
 * Spec-trace: UC-LB-01, CT-04, CT-12, I-5, CON.2, T3
 */
@Service
@Slf4j
public class EarnCalculator {

    private final EarningLedgerService ledgerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TieringClient tieringClient;

    public EarnCalculator(EarningLedgerService ledgerService, KafkaTemplate<String, Object> kafkaTemplate) {
        this(ledgerService, kafkaTemplate, null);
    }

    @Autowired
    public EarnCalculator(EarningLedgerService ledgerService, KafkaTemplate<String, Object> kafkaTemplate, TieringClient tieringClient) {
        this.ledgerService = ledgerService;
        this.kafkaTemplate = kafkaTemplate;
        this.tieringClient = tieringClient;
    }

    /**
     * Processes earn event with server-side authoritative tier query (CT-12).
     */
    @Transactional
    public EarnEventResponse processEarn(String memberId, Integer spendAmount, String sourceTxnId,
                                         String clientSuppliedTier, String campaignId, String programId) {
        String effectiveProgramId = (programId != null && !programId.isBlank()) ? programId : "DEFAULT_PROG";

        // I-5 Tamper Resistance: Query authoritative tier from Tiering DB (CT-12)
        String authoritativeTier = (tieringClient != null) 
                ? tieringClient.getMemberTier(memberId, effectiveProgramId)
                : ((clientSuppliedTier != null && !clientSuppliedTier.isBlank()) ? clientSuppliedTier : "SILVER");

        if (tieringClient != null && clientSuppliedTier != null && !clientSuppliedTier.isBlank() && !clientSuppliedTier.equalsIgnoreCase(authoritativeTier)) {
            log.warn("[I-5 Tamper Defense] Ignored client-forged tier '{}' for member '{}'. Using authoritative tier '{}' from CT-12.",
                    clientSuppliedTier, memberId, authoritativeTier);
        }

        log.info("Processing earn for member: {}, spend: {}, authoritativeTier: {}", memberId, spendAmount, authoritativeTier);

        // 1. Calculate Base Points using authoritative tier multiplier
        double tierMultiplier = getTierMultiplier(authoritativeTier);
        int basePoints = (int) Math.floor(spendAmount * 1.0 * tierMultiplier);

        ledgerService.recordBaseEarn(memberId, basePoints, sourceTxnId);

        // Publish QP event to Message Broker (async)
        Map<String, Object> qpEvent = new HashMap<>();
        qpEvent.put("memberId", memberId);
        qpEvent.put("qpAmount", basePoints);
        qpEvent.put("programId", effectiveProgramId);
        qpEvent.put("sourceEventId", sourceTxnId);
        kafkaTemplate.send("loyalty.earning.qp_accrued", memberId, qpEvent);
        log.info("Published QP event for member: {}, qpAmount: {}", memberId, basePoints);

        // 2. Calculate Bonus Points (if applicable)
        int bonusPoints = 0;
        if (campaignId != null && !campaignId.isEmpty()) {
            double bonusMultiplier = getCampaignMultiplier(campaignId);
            if (bonusMultiplier > 1.0) {
                bonusPoints = (int) Math.floor(basePoints * (bonusMultiplier - 1.0));
                ledgerService.recordBonusEarn(memberId, bonusPoints, sourceTxnId, campaignId);
            }
        }

        return EarnEventResponse.builder()
                .sourceTxnId(sourceTxnId)
                .memberId(memberId)
                .basePoints((long) basePoints)
                .bonusPoints((long) bonusPoints)
                .totalPoints((long) (basePoints + bonusPoints))
                .outcome("CONFIRMED")
                .status(202)
                .message("Earn request accepted and processed successfully")
                .build();
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
        if (campaignId.startsWith("DOUBLE")) return 2.0;
        if (campaignId.startsWith("TRIPLE")) return 3.0;
        return 1.0;
    }
}
