package com.loyalty.tiering_system.kafka;

import com.loyalty.tiering_system.event.QpAccruedEvent;
import com.loyalty.tiering_system.service.QpLedgerService;
import com.loyalty.tiering_system.service.TierUpgradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — lắng nghe event QP accrual từ Earning Engine.
 * Topic: loyalty.earning.qp_accrued
 * Orchestrates: QpLedgerService -> TierUpgradeService (FLOW-02)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QpAccruedConsumer {

    private final QpLedgerService qpLedgerService;
    private final TierUpgradeService tierUpgradeService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @KafkaListener(topics = "loyalty.earning.qp_accrued", groupId = "tiering-system-group")
    public void consume(String message) {
        try {
            QpAccruedEvent event = objectMapper.readValue(message, QpAccruedEvent.class);
            log.info("[QP Accrued] Received: memberId={}, programId={}, qpAmount={}, sourceEventId={}",
                    event.getMemberId(), event.getProgramId(), event.getQpAmount(), event.getSourceEventId());

            // 1. Ghi QP vào sổ cái và lấy tổng tích lũy
            long cumulativeQp = qpLedgerService.recordQpAndGetCumulative(
                    event.getMemberId(),
                    event.getProgramId(),
                    event.getSourceEventId(),
                    event.getQpAmount()
            );

            // 2. Đánh giá real-time upgrade (SLA ≤ 500ms)
            tierUpgradeService.evaluateAndUpgrade(event.getMemberId(), event.getProgramId(), cumulativeQp);
        } catch (Exception e) {
            log.error("Failed to process QP Accrued event", e);
        }
    }
}
