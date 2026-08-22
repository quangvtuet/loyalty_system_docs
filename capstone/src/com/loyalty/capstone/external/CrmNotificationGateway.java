package com.loyalty.capstone.external;

/** I-3 external 'CRM & Notification Gateway'. Mocked notification sink. */
public interface CrmNotificationGateway {
    void notifyMemberOfReversal(String memberId, String orderId);
    void notifyFinanceOfStaleReport(long dataAgeSeconds);
}
