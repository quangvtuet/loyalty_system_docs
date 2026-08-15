package com.loyalty.redemption_engine.api;

import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.service.CatalogService;
import com.loyalty.redemption_engine.service.RedemptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Redemption REST Controller — theo DD-03 Section 2.
 * FLOW-04: POST /api/v1/redemptions/orders
 * FLOW-09: Tier eligibility check embedded in order placement
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final CatalogService catalogService;

    // ─── Catalog API ───────────────────────────────────────────────

    /**
     * Lấy catalog phù hợp với tier của member (FR-03-002, FR-03-005).
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<RewardItem>> getCatalog(
            @RequestParam String programId,
            @RequestParam String memberTier) {
        List<RewardItem> items = catalogService.getCatalogForMember(programId, memberTier);
        return ResponseEntity.ok(items);
    }

    /**
     * Thêm item vào catalog (dùng khi seed data).
     */
    @PostMapping("/catalog/items")
    public ResponseEntity<RewardItem> addCatalogItem(@RequestBody AddItemRequest request) {
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
     * Tạo redemption order — FLOW-04 (UC-03-02).
     */
    @PostMapping("/redemptions/orders")
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        try {
            RedemptionOrder order = redemptionService.placeOrder(
                    request.getMemberId(),
                    request.getProgramId(),
                    UUID.fromString(request.getRewardItemId()),
                    request.getQuantity(),
                    request.getMemberTier(),
                    request.getAvailableBalance()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Xem lịch sử đổi điểm của member (FR-03-050, FR-03-051).
     */
    @GetMapping("/redemptions/orders")
    public ResponseEntity<List<RedemptionOrder>> getHistory(
            @RequestParam String memberId,
            @RequestParam String programId) {
        return ResponseEntity.ok(redemptionService.getHistory(memberId, programId));
    }

    /**
     * Hủy đơn (FR-03-043 — chỉ khi PENDING).
     */
    @DeleteMapping("/redemptions/orders/{orderId}")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderId,
            @RequestParam String memberId) {
        try {
            redemptionService.cancelOrder(UUID.fromString(orderId), memberId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Webhook callback khi fulfillment thành công (FR-03-032).
     */
    @PostMapping("/redemptions/orders/{orderId}/fulfill")
    public ResponseEntity<Void> fulfillOrder(@PathVariable String orderId) {
        redemptionService.fulfillOrder(UUID.fromString(orderId));
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook callback khi fulfillment thất bại — tự động reverse (FR-03-040, FR-03-041).
     */
    @PostMapping("/redemptions/orders/{orderId}/fail")
    public ResponseEntity<Void> failOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "FULFILLMENT_FAILED") String reason) {
        redemptionService.failAndReverseOrder(UUID.fromString(orderId), reason);
        return ResponseEntity.ok().build();
    }

    // ─── Request/Response DTOs ──────────────────────────────────────

    @Data
    static class PlaceOrderRequest {
        private String memberId;
        private String programId;
        private String rewardItemId;
        private int quantity = 1;
        private String memberTier;
        private long availableBalance;
    }

    @Data
    static class AddItemRequest {
        private String programId;
        private String name;
        private String category;
        private Long pointsCost;
        private double currencyValue;
        private String fulfillmentType = "DIGITAL";
        private String minTierRequired = "SILVER";
        private Integer stockQuantity;
    }

    @Data
    static class ErrorResponse {
        private final String error;
    }
}
