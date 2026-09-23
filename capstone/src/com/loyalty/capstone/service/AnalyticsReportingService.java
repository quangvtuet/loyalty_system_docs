package com.loyalty.capstone.service;

import com.loyalty.capstone.broker.MessageBroker;
import com.loyalty.capstone.broker.Topics;
import com.loyalty.capstone.domain.FactPointTransaction;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.external.CrmNotificationGateway;
import com.loyalty.capstone.store.DataWarehouse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * I-4 container 'Analytics & Reporting Service'. Sole writer of Data Warehouse.
 * It never reads a transactional store; it only consumes change events (CON.4).
 */
public final class AnalyticsReportingService {

    public static final String CONTAINER = "Analytics & Reporting Service";
    public static final long STALENESS_LIMIT_SECONDS = 600L;

    private final DataWarehouse dataWarehouse;
    private final ProgramManagementService programManagementService;
    private final CrmNotificationGateway crmNotificationGateway;
    private final Clock clock;
    private final String programId;

    public AnalyticsReportingService(DataWarehouse dataWarehouse,
                                     ProgramManagementService programManagementService,
                                     CrmNotificationGateway crmNotificationGateway,
                                     MessageBroker messageBroker, Clock clock, String programId) {
        this.dataWarehouse = dataWarehouse;
        this.programManagementService = programManagementService;
        this.crmNotificationGateway = crmNotificationGateway;
        this.clock = clock;
        this.programId = programId;
        messageBroker.subscribe(Topics.CDC_PLATFORM_EVENTS, this::onPlatformChangeEvent);
    }

    private void onPlatformChangeEvent(Map<String, Object> event) {
        FactPointTransaction fact = new FactPointTransaction(
                String.valueOf(event.get("pointTransactionId")),
                String.valueOf(event.get("memberId")),
                ((Number) event.get("outstandingPoints")).longValue(),
                clock.instant());
        dataWarehouse.upsertFact(CONTAINER, fact);
    }

    /** UC-LB-04. When the warehouse is more than ten minutes behind, the report says so and Finance is alerted. */
    public PointLiabilityReport pointLiabilityReport() {
        long outstanding = 0L;
        for (FactPointTransaction fact : dataWarehouse.facts()) {
            outstanding += fact.outstandingPoints;
        }
        LoyaltyProgram program = programManagementService.program(programId);
        double liability = outstanding * program.costPerPointUsd;

        Instant lastIngest = dataWarehouse.lastIngestedAt();
        long ageSeconds = lastIngest == null ? Long.MAX_VALUE
                : Duration.between(lastIngest, clock.instant()).getSeconds();
        boolean stale = ageSeconds > STALENESS_LIMIT_SECONDS;

        if (stale) {
            crmNotificationGateway.notifyFinanceOfStaleReport(ageSeconds);
        }
        return new PointLiabilityReport(outstanding, liability, stale, ageSeconds);
    }
}
