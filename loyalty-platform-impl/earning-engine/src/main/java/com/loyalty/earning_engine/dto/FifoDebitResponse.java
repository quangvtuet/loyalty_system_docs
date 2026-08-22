package com.loyalty.earning_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FifoDebitResponse {
    private String orderId;
    private String memberId;
    private Long pointsDebited;
    private String status; // RESERVED
    private Integer debitedBatchesCount;
    private String allocationId;
    private String message;
}
