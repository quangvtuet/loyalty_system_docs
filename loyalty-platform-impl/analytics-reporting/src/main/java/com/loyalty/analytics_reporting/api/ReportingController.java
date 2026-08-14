package com.loyalty.analytics_reporting.api;

import com.loyalty.analytics_reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Analytics REST Controller
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/liability/{programId}")
    public ResponseEntity<ReportingService.LiabilityReport> getLiabilityReport(@PathVariable UUID programId) {
        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);
        return ResponseEntity.ok(report);
    }
}
