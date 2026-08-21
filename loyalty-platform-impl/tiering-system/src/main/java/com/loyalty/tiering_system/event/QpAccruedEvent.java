package com.loyalty.tiering_system.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event đến từ Earning Engine — topic: loyalty.earning.qp_accrued
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QpAccruedEvent {
    private String memberId;
    private String programId;
    private String sourceEventId;
    private Long qpAmount;  // QP tích lũy từ giao dịch này
}
