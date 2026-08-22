package com.loyalty.analytics_reporting.api;

import com.loyalty.analytics_reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Analytics REST Controller — UC-LB-04 Generate point liability report.
 *
 * CT-26: API Gateway → Analytics & Reporting Service (Sync/HTTPS REST, GenerateReport).
 * CON.4: When report.isStale() is true the response still has HTTP 200, but the payload
 * includes stale=true and stalenessWarning. Finance must NOT treat the figures as current.
 * I-9: All data is read from the Data Warehouse only (FactPointTransactionRepository).
 *      This controller never queries Earning DB, Tiering DB, Redemption DB, or Program Mgmt DB.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    /**
     * UC-LB-04 — Generate point liability report.
     * OpenAPI operationId: getPointLiabilityReport
     *
     * Happy path: returns a fresh LiabilityReport (stale=false).
     * Alt CON.4 (EXC-07, G6-A05): returns LiabilityReport with stale=true and
     * stalenessWarning="CON.4: Data Warehouse facts are more than 10 minutes old..."
     */
    @GetMapping("/liability/{programId}")
    public ResponseEntity<ReportingService.LiabilityReport> getLiabilityReport(
            @PathVariable UUID programId) {
        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);
        // HTTP 200 in both fresh and stale cases; caller must inspect report.isStale() (CON.4)
        return ResponseEntity.ok(report);
    }
}

