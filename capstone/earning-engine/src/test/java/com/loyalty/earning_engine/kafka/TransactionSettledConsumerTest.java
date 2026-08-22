package com.loyalty.earning_engine.kafka;

import com.loyalty.earning_engine.dto.TransactionSettledEvent;
import com.loyalty.earning_engine.service.EarnCalculator;
import com.loyalty.earning_engine.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionSettledConsumer (Lab 10 UC-LB-01 Kafka Consumer Path).
 *
 * SUT: Earning Engine Service (Edge & Ingestion Zone message consumer)
 * Spec-trace: UC-LB-01, G3 (Async Kafka consume), CON.1
 */
class TransactionSettledConsumerTest {

    @Mock
    private EarnCalculator earnCalculator;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private TransactionSettledConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("UC-LB-01 Async: Consuming settled transaction event processes earn via EarnCalculator")
    void testConsume_HappyPath_ProcessesEarn() {
        TransactionSettledEvent event = TransactionSettledEvent.builder()
                .memberId("member-001")
                .amount(BigDecimal.valueOf(1000))
                .sourceTxnId("corebank-txn-001")
                .tier("GOLD")
                .build();

        when(idempotencyService.isDuplicate("corebank-txn-001", "SYS")).thenReturn(false);

        consumer.consume(event);

        verify(earnCalculator, times(1)).processEarn(
                eq("member-001"),
                eq(1000),
                eq("corebank-txn-001"),
                eq("GOLD"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("UC-LB-01 Async Alt: Duplicate settled transaction event is skipped under CON.1")
    void testConsume_DuplicateEvent_Skipped() {
        TransactionSettledEvent event = TransactionSettledEvent.builder()
                .memberId("member-001")
                .amount(BigDecimal.valueOf(1000))
                .sourceTxnId("corebank-txn-dup")
                .tier("SILVER")
                .build();

        when(idempotencyService.isDuplicate("corebank-txn-dup", "SYS")).thenReturn(true);

        consumer.consume(event);

        verify(earnCalculator, never()).processEarn(anyString(), anyInt(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("UC-LB-01 Async: Exception in earn processing is caught and handled safely")
    void testConsume_ExceptionHandledSafely() {
        TransactionSettledEvent event = TransactionSettledEvent.builder()
                .memberId("member-001")
                .amount(BigDecimal.valueOf(1000))
                .sourceTxnId("corebank-txn-err")
                .tier("SILVER")
                .build();

        when(idempotencyService.isDuplicate("corebank-txn-err", "SYS")).thenReturn(false);
        doThrow(new RuntimeException("Database error"))
                .when(earnCalculator).processEarn(anyString(), anyInt(), anyString(), anyString(), any(), any());

        // Must not throw out of consumer
        consumer.consume(event);
    }
}
