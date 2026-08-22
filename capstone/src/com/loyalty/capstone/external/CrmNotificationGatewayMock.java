package com.loyalty.capstone.external;

import java.util.ArrayList;
import java.util.List;

/** Fake for I-3 'CRM & Notification Gateway'. Records what would have been sent. */
public final class CrmNotificationGatewayMock implements CrmNotificationGateway {

    private final List<String> reversalNotices = new ArrayList<>();
    private final List<String> staleReportAlerts = new ArrayList<>();

    @Override
    public void notifyMemberOfReversal(String memberId, String orderId) {
        reversalNotices.add(memberId + ":" + orderId);
    }

    @Override
    public void notifyFinanceOfStaleReport(long dataAgeSeconds) {
        staleReportAlerts.add("stale:" + dataAgeSeconds);
    }

    public List<String> reversalNotices() { return reversalNotices; }

    public List<String> staleReportAlerts() { return staleReportAlerts; }
}
