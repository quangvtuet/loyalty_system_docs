package com.loyalty.earning_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarnEventResponse {
    private String sourceTxnId;
    private String memberId;
    private Long basePoints;
    private Long bonusPoints;
    private Long totalPoints;
    private String outcome;
    private Integer status;
    private String message;
}
