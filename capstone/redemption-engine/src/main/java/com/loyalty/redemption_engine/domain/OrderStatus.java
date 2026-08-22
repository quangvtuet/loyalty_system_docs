package com.loyalty.redemption_engine.domain;

public enum OrderStatus {
    PENDING,      // Đơn vừa tạo, điểm đang PENDING_DEBIT
    IN_PROGRESS,  // Fulfillment đang xử lý
    FULFILLED,    // Thành công, điểm CONFIRMED_DEBIT
    FAILED,       // Fulfillment thất bại
    CANCELLED,    // Member hủy (chỉ khi PENDING)
    REVERSED      // Đã hoàn điểm sau khi FAILED
}
