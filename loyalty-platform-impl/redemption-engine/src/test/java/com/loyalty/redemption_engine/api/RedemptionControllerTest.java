package com.loyalty.redemption_engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.redemption_engine.domain.OrderStatus;
import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.service.CatalogService;
import com.loyalty.redemption_engine.service.RedemptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller-layer tests for RedemptionController — UC-LB-02.
 *
 * Asserts HTTP status codes and response body against the OpenAPI contract,
 * closing T3 and T5 capstone scoring gaps at the HTTP layer.
 *
 * Spec-trace:
 * - testPlaceOrder_HappyPath_Returns201                     → UC-LB-02 happy path, HTTP 201, I-11
 * - testPlaceOrder_InsufficientBalance_Returns422           → ALT-01 / ERR_RED_INSUFFICIENT_BALANCE: HTTP 422, T3, T5
 * - testPlaceOrder_TierEligibilityFailed_Returns422         → ALT-02 / ERR_RED_TIER_ELIGIBILITY_FAILED: HTTP 422, T3
 * - testPlaceOrder_MinimumPoints_Returns400                 → ALT-03 / ERR_RED_MIN_POINTS: HTTP 400, T5
 * - testFailOrder_CON3_CompensatingAction_Returns200        → CON.3 ALT-05: HTTP 200, G5 confirmed, T5
 *
 * I-3 mocked: RedemptionService and CatalogService via @MockitoBean — no live DB/Redis.
 */
@WebMvcTest(RedemptionController.class)
class RedemptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RedemptionService redemptionService;

    @MockitoBean
    private CatalogService catalogService;

    private static final UUID ITEM_ID = UUID.randomUUID();

    /**
     * UC-LB-02 happy path at the HTTP layer.
     * POST /api/v1/redemptions/orders → HTTP 201 Created.
     * OpenAPI operationId: createRedemptionOrder.
     * Spec-trace: I-11, T5
     */
    @Test
    void testPlaceOrder_HappyPath_Returns201() throws Exception {
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(UUID.randomUUID())
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.PENDING)
                .totalPointsDebited(300L)
                .build();

        when(redemptionService.placeOrder(any(), any(), any(), anyInt(), any(), anyLong()))
                .thenReturn(order);

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1,
                "memberTier", "SILVER",
                "availableBalance", 500L
        );

        // Act & Assert: HTTP 201 — matches OpenAPI createRedemptionOrder 201 response
        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * UC-LB-02 ALT-01 — insufficient balance at the HTTP layer.
     * POST /api/v1/redemptions/orders → HTTP 422 Unprocessable Entity.
     * OpenAPI operationId: createRedemptionOrder, response 422 (ERR_RED_INSUFFICIENT_BALANCE).
     *
     * Attempts the I-5 hard rule skip (T3): a caller sends an order with balance < cost;
     * the runtime must reject with 422 — no points debited.
     * Spec-trace: G6-A02, ALT-01, I-5, T3, T5
     */
    @Test
    void testPlaceOrder_InsufficientBalance_Returns422() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt(), any(), anyLong()))
                .thenThrow(new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1,
                "memberTier", "SILVER",
                "availableBalance", 100L   // insufficient — item costs 300
        );

        // Act & Assert: HTTP 422 — matches OpenAPI createRedemptionOrder 422 response
        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ERR_RED_INSUFFICIENT_BALANCE"));
    }

    /**
     * UC-LB-02 ALT-02 — tier eligibility failure at the HTTP layer.
     * POST /api/v1/redemptions/orders → HTTP 422 Unprocessable Entity.
     * OpenAPI operationId: createRedemptionOrder, response 422 (ERR_RED_TIER_ELIGIBILITY_FAILED).
     * Spec-trace: G6-A02, ALT-02, I-5, T3, T5
     */
    @Test
    void testPlaceOrder_TierEligibilityFailed_Returns422() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt(), any(), anyLong()))
                .thenThrow(new IllegalStateException("ERR_RED_TIER_ELIGIBILITY_FAILED"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1,
                "memberTier", "SILVER",    // item requires PLATINUM
                "availableBalance", 10000L
        );

        // Act & Assert: HTTP 422
        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ERR_RED_TIER_ELIGIBILITY_FAILED"));
    }

    /**
     * UC-LB-02 ALT-03 — below minimum points at the HTTP layer.
     * POST /api/v1/redemptions/orders → HTTP 400 Bad Request.
     * OpenAPI operationId: createRedemptionOrder, response 400 (ERR_RED_MIN_POINTS).
     * Spec-trace: ALT-03, T5
     */
    @Test
    void testPlaceOrder_MinimumPoints_Returns400() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt(), any(), anyLong()))
                .thenThrow(new IllegalArgumentException("ERR_RED_MIN_POINTS"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1,
                "memberTier", "SILVER",
                "availableBalance", 500L
        );

        // Act & Assert: HTTP 400 — matches OpenAPI createRedemptionOrder 400 response
        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_RED_MIN_POINTS"));
    }

    /**
     * UC-LB-02 ALT-05 / CON.3 — partner fulfillment failure + compensating action.
     * POST /api/v1/redemptions/orders/{orderId}/fail → HTTP 200.
     * G5: compensating action (reversal) confirmed — redemptionService.failAndReverseOrder() is called.
     * Spec-trace: G6-A03, G6-T04/T05, CON.3, EXC-05, ALT-05, G5, T5
     */
    @Test
    void testFailOrder_CON3_CompensatingAction_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        doNothing().when(redemptionService).failAndReverseOrder(eq(orderId), eq("OUT_OF_STOCK"));

        // Act & Assert: HTTP 200 — matches OpenAPI failAndReverseRedemptionOrder 200 response
        mockMvc.perform(post("/api/v1/redemptions/orders/{orderId}/fail", orderId)
                        .param("reason", "OUT_OF_STOCK"))
                .andExpect(status().isOk());

        // G5 compensating action: failAndReverseOrder is called — CON.3 reversal triggered
        verify(redemptionService).failAndReverseOrder(eq(orderId), eq("OUT_OF_STOCK"));
    }
}
