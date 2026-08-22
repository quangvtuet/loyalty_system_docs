package com.loyalty.earning_engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.earning_engine.dto.FifoDebitRequest;
import com.loyalty.earning_engine.dto.FifoDebitResponse;
import com.loyalty.earning_engine.dto.FifoRestoreRequest;
import com.loyalty.earning_engine.dto.FifoRestoreResponse;
import com.loyalty.earning_engine.service.EarningLedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for CT-13 operations in FifoDebitController.
 * SUT: Earning Engine Service
 */
@WebMvcTest(FifoDebitController.class)
class FifoDebitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EarningLedgerService earningLedgerService;

    @Test
    void testDebitFifo_Success_Returns200() throws Exception {
        FifoDebitRequest request = FifoDebitRequest.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .pointsRequired(500L)
                .orderId("order-123")
                .build();

        FifoDebitResponse response = FifoDebitResponse.builder()
                .orderId("order-123")
                .memberId("member-001")
                .pointsDebited(500L)
                .status("RESERVED")
                .debitedBatchesCount(2)
                .message("FIFO debit reserved successfully")
                .build();

        when(earningLedgerService.debitPointsFifo(eq("member-001"), eq("DEFAULT_PROG"), eq(500L), eq("order-123")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/earning/fifo/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.pointsDebited").value(500))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void testDebitFifo_InsufficientBalance_Returns422() throws Exception {
        FifoDebitRequest request = FifoDebitRequest.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .pointsRequired(5000L)
                .orderId("order-123")
                .build();

        when(earningLedgerService.debitPointsFifo(eq("member-001"), eq("DEFAULT_PROG"), eq(5000L), eq("order-123")))
                .thenThrow(new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE: Required 5000 but available 100"));

        mockMvc.perform(post("/api/v1/earning/fifo/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient Balance"))
                .andExpect(jsonPath("$.detail").value("ERR_RED_INSUFFICIENT_BALANCE: Required 5000 but available 100"));
    }

    @Test
    void testRestoreFifo_Success_Returns200() throws Exception {
        FifoRestoreRequest request = FifoRestoreRequest.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .orderId("order-123")
                .pointsToRestore(500L)
                .reason("PARTNER_FAILURE")
                .build();

        FifoRestoreResponse response = FifoRestoreResponse.builder()
                .orderId("order-123")
                .memberId("member-001")
                .pointsRestored(500L)
                .status("RESTORED")
                .reason("PARTNER_FAILURE")
                .message("Points restored successfully")
                .build();

        when(earningLedgerService.restorePoints(eq("member-001"), eq("DEFAULT_PROG"), eq("order-123"), eq(500L), eq("PARTNER_FAILURE")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/earning/fifo/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.pointsRestored").value(500))
                .andExpect(jsonPath("$.status").value("RESTORED"));
    }

    @Test
    void testGetBalance_Returns200() throws Exception {
        when(earningLedgerService.getMemberBalance("member-001", "DEFAULT_PROG")).thenReturn(1500L);

        mockMvc.perform(get("/api/v1/earning/members/member-001/balance")
                        .param("programId", "DEFAULT_PROG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("member-001"))
                .andExpect(jsonPath("$.confirmedBalance").value(1500));
    }
}
