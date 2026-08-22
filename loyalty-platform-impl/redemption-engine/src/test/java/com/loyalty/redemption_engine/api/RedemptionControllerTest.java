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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller-layer tests for RedemptionController — UC-LB-02.
 *
 * Verifies OpenAPI contract alignment with exact HTTP methods (POST, PATCH),
 * response status codes (201, 200, 400, 409, 422), and ProblemDetail structures.
 *
 * Spec-trace:
 * - testPlaceOrder_HappyPath_Returns201                     → G6-T01, HTTP 201, IN_PROGRESS
 * - testPlaceOrder_InsufficientBalance_Returns422           → G6-A02, ALT-01 / ERR_RED_INSUFFICIENT_BALANCE: HTTP 422
 * - testPlaceOrder_TierEligibilityFailed_Returns422         → G6-A02, ALT-02 / ERR_RED_TIER_ELIGIBILITY_FAILED: HTTP 422
 * - testPlaceOrder_MinimumPoints_Returns400                 → ALT-03 / ERR_RED_MIN_POINTS: HTTP 400
 * - testFulfillOrder_HappyPath_Returns200                   → G6-T03: PATCH fulfill, HTTP 200, FULFILLED
 * - testFailOrder_CON3_CompensatingAction_Returns200        → G6-T04/T05, G6-A03: PATCH fail, HTTP 200, REVERSED
 * - testCancelOrder_WhenPending_Returns200                 → G6-T02: PATCH cancel, HTTP 200, CANCELLED
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
     * G6-T01: Order creation happy path at HTTP layer.
     * Client submits order without client-supplied balance or tier fields.
     * Response: HTTP 201 with status IN_PROGRESS.
     */
    @Test
    void testPlaceOrder_HappyPath_Returns201() throws Exception {
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(UUID.randomUUID())
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .rewardItemId(ITEM_ID)
                .quantity(1)
                .status(OrderStatus.IN_PROGRESS)
                .totalPointsDebited(300L)
                .memberTierAtOrder("SILVER")
                .build();

        when(redemptionService.placeOrder(eq("member-001"), eq("DEFAULT_PROG"), eq(ITEM_ID), eq(1)))
                .thenReturn(order);

        // Client body contains ONLY memberId, programId, rewardItemId, quantity (I-5 invariant)
        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1
        );

        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalPointsDebited").value(300));
    }

    /**
     * ALT-01: Insufficient balance in Earning DB → HTTP 422 with ProblemDetail.
     */
    @Test
    void testPlaceOrder_InsufficientBalance_Returns422() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt()))
                .thenThrow(new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE: Required 300 but available 100"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1
        );

        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value("ERR_RED_INSUFFICIENT_BALANCE: Required 300 but available 100"));
    }

    /**
     * ALT-02: Tier eligibility failure → HTTP 422 with ProblemDetail.
     */
    @Test
    void testPlaceOrder_TierEligibilityFailed_Returns422() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt()))
                .thenThrow(new IllegalStateException("ERR_RED_TIER_ELIGIBILITY_FAILED: SILVER is below PLATINUM"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1
        );

        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value("ERR_RED_TIER_ELIGIBILITY_FAILED: SILVER is below PLATINUM"));
    }

    /**
     * ALT-03: Minimum points violation → HTTP 400 Bad Request.
     */
    @Test
    void testPlaceOrder_MinimumPoints_Returns400() throws Exception {
        when(redemptionService.placeOrder(any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("ERR_RED_MIN_POINTS: Minimum redemption is 100 points"));

        Map<String, Object> body = Map.of(
                "memberId", "member-001",
                "programId", "DEFAULT_PROG",
                "rewardItemId", ITEM_ID.toString(),
                "quantity", 1
        );

        mockMvc.perform(post("/api/v1/redemptions/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("ERR_RED_MIN_POINTS: Minimum redemption is 100 points"));
    }

    /**
     * G6-T03: PATCH /api/v1/redemptions/orders/{orderId}/fulfill → HTTP 200 FULFILLED.
     */
    @Test
    void testFulfillOrder_HappyPath_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder fulfilled = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .status(OrderStatus.FULFILLED)
                .build();

        when(redemptionService.fulfillOrder(eq(orderId))).thenReturn(fulfilled);

        mockMvc.perform(patch("/api/v1/redemptions/orders/{orderId}/fulfill", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        verify(redemptionService).fulfillOrder(eq(orderId));
    }

    /**
     * G6-T04 / G6-T05 / G6-A03: PATCH /api/v1/redemptions/orders/{orderId}/fail → HTTP 200 REVERSED.
     */
    @Test
    void testFailOrder_CON3_CompensatingAction_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder reversed = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .status(OrderStatus.REVERSED)
                .failureReason("OUT_OF_STOCK")
                .build();

        when(redemptionService.failAndReverseOrder(eq(orderId), eq("OUT_OF_STOCK"))).thenReturn(reversed);

        Map<String, String> body = Map.of("reason", "OUT_OF_STOCK");

        mockMvc.perform(patch("/api/v1/redemptions/orders/{orderId}/fail", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"))
                .andExpect(jsonPath("$.failureReason").value("OUT_OF_STOCK"));

        verify(redemptionService).failAndReverseOrder(eq(orderId), eq("OUT_OF_STOCK"));
    }

    /**
     * G6-T02: PATCH /api/v1/redemptions/orders/{orderId}/cancel → HTTP 200 CANCELLED.
     */
    @Test
    void testCancelOrder_WhenPending_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder cancelled = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .status(OrderStatus.CANCELLED)
                .build();

        when(redemptionService.cancelOrder(eq(orderId), eq("member-001"))).thenReturn(cancelled);

        Map<String, String> body = Map.of("memberId", "member-001");

        mockMvc.perform(patch("/api/v1/redemptions/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(redemptionService).cancelOrder(eq(orderId), eq("member-001"));
    }
}
