package com.loyalty.redemption_engine.api;

import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.service.CatalogService;
import com.loyalty.redemption_engine.service.RedemptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Redemption REST Controller — UC-LB-02 Redeem reward with FIFO.
 *
 * Contract CT-11: API Gateway → Redemption Engine Service (Sync/HTTPS REST).
 * Operations:
 * - CreateRedemptionOrder (POST /api/v1/redemptions/orders)
 * - FulfillRedemptionOrder (PATCH /api/v1/redemptions/orders/{orderId}/fulfill)
 * - FailAndReverseRedemptionOrder (PATCH /api/v1/redemptions/orders/{orderId}/fail)
 * - CancelRedemptionOrder (PATCH /api/v1/redemptions/orders/{orderId}/cancel)
 *
 * SUT: Redemption Engine Service
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final CatalogService catalogService;

    // ─── Catalog API ───────────────────────────────────────────────

    @GetMapping("/catalog")
    public ResponseEntity<List<RewardItem>> getCatalog(
            @RequestParam String programId,
            @RequestParam String memberTier) {
        List<RewardItem> items = catalogService.getCatalogForMember(programId, memberTier);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/catalog/items")
    public ResponseEntity<RewardItem> addCatalogItem(@Valid @RequestBody AddItemRequest request) {
        RewardItem item = RewardItem.builder()
                .programId(request.getProgramId())
                .name(request.getName())
                .category(request.getCategory())
                .pointsCost(request.getPointsCost())
                .currencyValue(BigDecimal.valueOf(request.getCurrencyValue()))
                .fulfillmentType(com.loyalty.redemption_engine.domain.FulfillmentType.valueOf(request.getFulfillmentType()))
                .minTierRequired(request.getMinTierRequired())
                .stockQuantity(request.getStockQuantity())
                .status("ACTIVE")
                .build();
        item = catalogService.addItem(item);
        log.info("[Catalog] Item added: {} ({} pts, tier: {})", item.getName(), item.getPointsCost(), item.getMinTierRequired());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    // ─── Redemption Order API ───────────────────────────────────────

    /**
     * POST /api/v1/redemptions/orders (operationId: createRedemptionOrder).
     * Server validates balance & tier from I-7 sources of truth (CT-12, CT-13).
     */
    @PostMapping("/redemptions/orders")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        try {
            RedemptionOrder order = redemptionService.placeOrder(
                    request.getMemberId(),
                    request.getProgramId(),
                    UUID.fromString(request.getRewardItemId()),
                    request.getQuantity()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            HttpStatus status = msg.contains("ERR_RED_CONCURRENT_REQUEST") ? HttpStatus.CONFLICT
                    : (msg.startsWith("ERR_RED_TIER_SERVICE_UNAVAILABLE") || msg.startsWith("ERR_RED_EARNING_SERVICE_UNAVAILABLE")
                    ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNPROCESSABLE_ENTITY);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, msg);
            problem.setType(URI.create("https://loyalty.internal/errors/" + (status == HttpStatus.CONFLICT ? "concurrent-request" :
                    status == HttpStatus.SERVICE_UNAVAILABLE ? "upstream-unavailable" : "validation-failed")));
            problem.setTitle(status == HttpStatus.CONFLICT ? "Balance Lock Busy" :
                    status == HttpStatus.SERVICE_UNAVAILABLE ? "Upstream Service Unavailable" : "Validation Failed");
            return ResponseEntity.status(status).body(problem);
        } catch (IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/bad-request"));
            problem.setTitle("Bad Request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }
    }

    @GetMapping("/redemptions/orders")
    public ResponseEntity<List<RedemptionOrder>> getHistory(
            @RequestParam String memberId,
            @RequestParam String programId) {
        return ResponseEntity.ok(redemptionService.getHistory(memberId, programId));
    }

    /**
     * PATCH /api/v1/redemptions/orders/{orderId}/cancel (operationId: cancelRedemptionOrder).
     */
    @PatchMapping("/redemptions/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        try {
            String memberId = (request != null && request.getMemberId() != null) ? request.getMemberId() : "member-001";
            RedemptionOrder order = redemptionService.cancelOrder(UUID.fromString(orderId), memberId);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/cancel-not-allowed"));
            problem.setTitle("Cancellation Not Allowed");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        } catch (IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/order-not-found"));
            problem.setTitle("Order Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }
    }

    /**
     * PATCH /api/v1/redemptions/orders/{orderId}/fulfill (operationId: fulfillRedemptionOrder).
     */
    @PatchMapping("/redemptions/orders/{orderId}/fulfill")
    public ResponseEntity<?> fulfillOrder(@PathVariable String orderId) {
        try {
            RedemptionOrder order = redemptionService.fulfillOrder(UUID.fromString(orderId));
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/order-not-found"));
            problem.setTitle("Order Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }
    }

    /**
     * PATCH /api/v1/redemptions/orders/{orderId}/fail (operationId: failAndReverseRedemptionOrder).
     */
    @PatchMapping("/redemptions/orders/{orderId}/fail")
    public ResponseEntity<?> failOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) FailOrderRequest request) {
        try {
            String reason = (request != null && request.getReason() != null) ? request.getReason() : "FULFILLMENT_FAILED";
            RedemptionOrder order = redemptionService.failAndReverseOrder(UUID.fromString(orderId), reason);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            problem.setType(URI.create("https://loyalty.internal/errors/order-not-found"));
            problem.setTitle("Order Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }
    }

    // ─── Request DTOs ──────────────────────────────────────────────

    @Data
    public static class PlaceOrderRequest {
        @NotBlank(message = "memberId is required")
        private String memberId;
        private String programId = "DEFAULT_PROG";
        @NotBlank(message = "rewardItemId is required")
        private String rewardItemId;
        @Min(value = 1, message = "quantity must be at least 1")
        private int quantity = 1;
    }

    @Data
    public static class CancelOrderRequest {
        private String memberId;
    }

    @Data
    public static class FailOrderRequest {
        private String reason;
    }

    @Data
    public static class AddItemRequest {
        private String programId = "DEFAULT_PROG";
        @NotBlank
        private String name;
        private String category;
        @NotNull
        private Long pointsCost;
        private double currencyValue;
        private String fulfillmentType = "DIGITAL";
        private String minTierRequired = "SILVER";
        private Integer stockQuantity;
    }
}
