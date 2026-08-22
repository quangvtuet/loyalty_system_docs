package com.loyalty.earning_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FifoRestoreResponse {
    private String orderId;
    private String memberId;
    private Long pointsRestored;
    private String status; // RESTORED
    private String reason;
    private String message;
    private String allocationId;
}
