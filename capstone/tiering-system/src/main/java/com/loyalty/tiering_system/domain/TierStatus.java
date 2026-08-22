package com.loyalty.tiering_system.domain;

/**
 * Trạng thái của member_tier — theo thiết kế DD-02 Grace Period State Machine.
 */
public enum TierStatus {
    ACTIVE,           // Tier đang hoạt động bình thường
    IN_GRACE_PERIOD,  // Đang trong giai đoạn gia hạn 30 ngày trước khi downgrade
    DOWNGRADED        // Đã bị hạ bậc sau khi hết grace period
}
