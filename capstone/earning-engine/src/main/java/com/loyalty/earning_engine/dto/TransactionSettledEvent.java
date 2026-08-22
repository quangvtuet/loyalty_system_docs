package com.loyalty.earning_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSettledEvent {
    private String sourceTxnId;
    private String memberId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private LocalDateTime settledTs;
    private String tier; // Typically retrieved from Tiering System, we add here for demo
}
