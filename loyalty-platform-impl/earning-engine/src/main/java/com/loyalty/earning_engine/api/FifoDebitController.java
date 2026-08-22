package com.loyalty.earning_engine.api;

import com.loyalty.earning_engine.dto.FifoDebitRequest;
import com.loyalty.earning_engine.dto.FifoDebitResponse;
import com.loyalty.earning_engine.dto.FifoRestoreRequest;
import com.loyalty.earning_engine.dto.FifoRestoreResponse;
import com.loyalty.earning_engine.service.EarningLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * FIFO Debit & Restoration REST Controller — UC-LB-02 (CT-13).
 *
 * Contract CT-13: Redemption Engine Service → Earning Engine Service (Sync/HTTPS REST).
 * Operations:
 * - DebitPointsFifo (POST /api/v1/earning/fifo/debit)
 * - RestorePoints (POST /api/v1/earning/fifo/restore)
 * - GetMemberBalance (GET /api/v1/earning/members/{memberId}/balance)
 *
 * SUT: Earning Engine Service
 */
@RestController
@RequestMapping("/api/v1/earning")
@RequiredArgsConstructor
public class FifoDebitController {

    private final EarningLedgerService earningLedgerService;

    /**
     * CT-13: DebitPointsFifo
     * Trừ điểm theo thuật toán FIFO từ các lô điểm cũ nhất.
     */
    @PostMapping("/fifo/debit")
    public ResponseEntity<?> debitFifo(@Valid @RequestBody FifoDebitRequest request) {
        try {
            FifoDebitResponse response = earningLedgerService.debitPointsFifo(
                    request.getMemberId(),
                    request.getProgramId(),
                    request.getPointsRequired(),
                    request.getOrderId()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/insufficient-balance"));
            problem.setTitle("Insufficient Balance");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
        }
    }

    /**
     * CT-13: RestorePoints
     * Tự động hoàn điểm khi fulfillment thất bại (CON.3 / EXC-05).
     */
    @PostMapping("/fifo/restore")
    public ResponseEntity<?> restoreFifo(@Valid @RequestBody FifoRestoreRequest request) {
        try {
            FifoRestoreResponse response = earningLedgerService.restorePoints(
                request.getMemberId(),
                request.getProgramId(),
                request.getOrderId(),
                request.getPointsToRestore(),
                request.getReason()
        );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/restore-failed"));
            problem.setTitle("Restore Failed");
            return ResponseEntity.unprocessableEntity().body(problem);
        }
    }

    /**
     * CT-13 read path: Query current confirmed balance from Earning DB.
     */
    @GetMapping("/members/{memberId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "DEFAULT_PROG") String programId) {
        Long balance = earningLedgerService.getMemberBalance(memberId, programId);
        return ResponseEntity.ok(Map.of(
                "memberId", memberId,
                "programId", programId,
                "confirmedBalance", balance
        ));
    }
}
