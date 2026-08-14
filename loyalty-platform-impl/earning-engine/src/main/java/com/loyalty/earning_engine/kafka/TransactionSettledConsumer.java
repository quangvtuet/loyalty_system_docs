package com.loyalty.earning_engine.kafka;

import com.loyalty.earning_engine.dto.TransactionSettledEvent;
import com.loyalty.earning_engine.service.EarnCalculator;
import com.loyalty.earning_engine.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionSettledConsumer {

    private final EarnCalculator earnCalculator;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "corebanking.transactions.settled", groupId = "earning-engine-group")
    public void consume(TransactionSettledEvent event) {
        log.info("Received transaction event: {}", event.getSourceTxnId());
        
        try {
            boolean isDuplicate = idempotencyService.isDuplicate(event.getSourceTxnId(), "SYS");
            if (isDuplicate) {
                log.info("Skipping duplicate transaction: {}", event.getSourceTxnId());
                return;
            }

            earnCalculator.processEarn(
                    event.getMemberId(),
                    event.getAmount().intValue(),
                    event.getSourceTxnId(),
                    event.getTier() != null ? event.getTier() : "SILVER",
                    null // Default no campaign for raw settled event in this mock
            );
            log.info("Successfully processed transaction: {}", event.getSourceTxnId());
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", event.getSourceTxnId(), e.getMessage(), e);
            // In a real system, send to DLT (Dead Letter Topic)
        }
    }
}
