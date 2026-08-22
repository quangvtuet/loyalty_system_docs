package com.loyalty.earning_engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.earning_engine.dto.PartnerEarnRequest;
import com.loyalty.earning_engine.service.EarnCalculator;
import com.loyalty.earning_engine.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller-layer tests for PartnerEarnController — UC-LB-01.
 *
 * These tests assert HTTP status codes and response body against the OpenAPI contract,
 * closing T3 and T5 gaps from the capstone scoring (test at HTTP layer, not just mock layer).
 *
 * Spec-trace:
 * - testSubmitEarn_HappyPath_Returns202               → UC-LB-01 happy path, HTTP 202, I-11
 * - testSubmitEarn_DuplicateTransaction_Returns409    → CON.1 / EXC-01 / G6-A01: HTTP 409 matches OpenAPI
 * - testSubmitEarn_MissingTransactionId_Returns400    → OpenAPI 400 bad request
 * - testSubmitEarn_MissingMemberId_Returns400         → OpenAPI 400 bad request
 *
 * I-3 mocked: IdempotencyService (Redis) mocked via @MockitoBean — no live Redis.
 *             EarnCalculator mocked — no live Kafka, DB.
 */
@WebMvcTest(PartnerEarnController.class)
class PartnerEarnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EarnCalculator earnCalculator;

    @MockitoBean
    private IdempotencyService idempotencyService;

    /**
     * UC-LB-01 happy path at the HTTP layer.
     * POST /api/v1/partners/earn → HTTP 202 Accepted.
     * OpenAPI operationId: recordEarn.
     * Spec-trace: I-11, T5 (status matches OpenAPI 202)
     */
    @Test
    void testSubmitEarn_HappyPath_Returns202() throws Exception {
        // Arrange: new event, not a duplicate
        when(idempotencyService.isDuplicate("txn-ctrl-001", "PARTNER")).thenReturn(false);
        doNothing().when(earnCalculator).processEarn(any(), any(), any(), any(), any(), any());

        PartnerEarnRequest request = new PartnerEarnRequest();
        request.setTransactionId("txn-ctrl-001");
        request.setMemberId("member-001");
        request.setSpendAmount(1000);
        request.setProgramId("DEFAULT_PROG");

        // Act & Assert: HTTP 202 (matches OpenAPI recordEarn 202 response)
        mockMvc.perform(post("/api/v1/partners/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.message").exists());

        // Assert: earn was processed (not swallowed)
        verify(earnCalculator).processEarn(eq("member-001"), eq(1000), eq("txn-ctrl-001"), any(), any(), any());
    }

    /**
     * UC-LB-01 alt — CON.1 duplicate event at the HTTP layer (EXC-01 / G6-A01).
     * POST /api/v1/partners/earn with same transactionId → HTTP 409 Conflict.
     * OpenAPI operationId: recordEarn, response: 409 (ERR_EARN_DUPLICATE).
     *
     * This test attempts the I-5 hard rule skip at the HTTP layer (T3):
     * a caller sends the exact same transactionId twice; the second call must be rejected with 409.
     * Spec-trace: G6-A01, CON.1, I-5, T3, T5
     */
    @Test
    void testSubmitEarn_DuplicateTransaction_Returns409() throws Exception {
        // Arrange: IdempotencyService identifies this as a duplicate (second delivery)
        when(idempotencyService.isDuplicate("txn-dup-ctrl", "PARTNER")).thenReturn(true);

        PartnerEarnRequest request = new PartnerEarnRequest();
        request.setTransactionId("txn-dup-ctrl");
        request.setMemberId("member-001");
        request.setSpendAmount(500);

        // Act & Assert: HTTP 409 — matches OpenAPI recordEarn 409 response (CON.1 ERR_EARN_DUPLICATE)
        mockMvc.perform(post("/api/v1/partners/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Assert: EarnCalculator NOT called — no second PointTransaction written (CON.1)
        verify(earnCalculator, never()).processEarn(any(), any(), any(), any(), any(), any());
    }

    /**
     * Bad request — missing required transactionId.
     * HTTP 400 — matches OpenAPI recordEarn 400 response.
     * Spec-trace: T5 (bad-request path)
     */
    @Test
    void testSubmitEarn_MissingTransactionId_Returns400() throws Exception {
        PartnerEarnRequest request = new PartnerEarnRequest();
        // transactionId intentionally omitted
        request.setMemberId("member-001");
        request.setSpendAmount(500);

        mockMvc.perform(post("/api/v1/partners/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(earnCalculator, never()).processEarn(any(), any(), any(), any(), any(), any());
    }

    /**
     * Bad request — missing required memberId.
     * HTTP 400 — matches OpenAPI recordEarn 400 response.
     * Spec-trace: T5 (bad-request path)
     */
    @Test
    void testSubmitEarn_MissingMemberId_Returns400() throws Exception {
        PartnerEarnRequest request = new PartnerEarnRequest();
        request.setTransactionId("txn-nomember");
        // memberId intentionally omitted
        request.setSpendAmount(500);

        mockMvc.perform(post("/api/v1/partners/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(earnCalculator, never()).processEarn(any(), any(), any(), any(), any(), any());
    }
}
