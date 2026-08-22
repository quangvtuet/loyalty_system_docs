package com.loyalty.earning_engine.api;

import com.loyalty.earning_engine.dto.EarnEventResponse;
import com.loyalty.earning_engine.dto.PartnerEarnRequest;
import com.loyalty.earning_engine.service.EarnCalculator;
import com.loyalty.earning_engine.service.EarningLedgerService;
import com.loyalty.earning_engine.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Partner Earn Controller — UC-LB-01 Process settled earn event.
 *
 * Contract CT-04: API Gateway → Earning Engine Service (Sync/HTTPS REST, RecordEarn).
 * OpenAPI operationId: recordEarn, cancelEarnTransaction
 * SUT: Earning Engine Service
 */
@RestController
@RequestMapping("/api/v1/partners/earn")
@RequiredArgsConstructor
public class PartnerEarnController {

    private final EarnCalculator earnCalculator;
    private final EarningLedgerService earningLedgerService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<?> submitEarn(@Valid @RequestBody PartnerEarnRequest request) {
        
        boolean isDuplicate = idempotencyService.isDuplicate(request.getTransactionId(), "PARTNER");
        if (isDuplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", 409,
                    "error", "ERR_EARN_DUPLICATE",
                    "message", "Duplicate transaction detected under CON.1"
            ));
        }

        EarnEventResponse response = earnCalculator.processEarn(
                request.getMemberId(),
                request.getSpendAmount(),
                request.getTransactionId(),
                "SILVER", // Default tier for partner earn in this mock
                request.getCampaignId(),
                request.getProgramId() != null ? request.getProgramId() : "DEFAULT_PROG"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Reversal of rewarded earn event by Core Banking System (EXC-02, CON.1).
     * OpenAPI operationId: cancelEarnTransaction
     */
    @PatchMapping("/{sourceTxnId}/cancel")
    public ResponseEntity<?> cancelEarn(@PathVariable String sourceTxnId) {
        boolean cancelled = earningLedgerService.cancelTransaction(sourceTxnId);
        if (cancelled) {
            return ResponseEntity.ok(Map.of(
                    "sourceTxnId", sourceTxnId,
                    "outcome", "CANCELLED",
                    "status", 200,
                    "message", "PointTransaction cancelled and balance corrected under CON.1"
            ));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", "ERR_TRANSACTION_NOT_FOUND",
                "message", "Source transaction not found or already cancelled"
        ));
    }
}
