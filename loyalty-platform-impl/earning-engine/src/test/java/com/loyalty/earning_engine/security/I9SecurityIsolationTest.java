package com.loyalty.earning_engine.security;

import com.loyalty.earning_engine.api.FifoDebitController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.loyalty.earning_engine.service.EarningLedgerService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I-9 Zone Security & Data Ownership Isolation Test.
 *
 * Rule: external actors cannot write Earning DB through an unowned database endpoint.
 * The only exposed Earning write operations are the named CT-04 and CT-13 contracts.
 *
 * Spec-trace: I-9, CON.2, EXC-04
 */
@WebMvcTest(FifoDebitController.class)
class I9SecurityIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EarningLedgerService earningLedgerService;

    /**
     * I-9 / CON.2 / EXC-04 negative test:
     * Attempt a direct database write through an unowned HTTP endpoint. The application
     * must reject the attempt because no such route is part of the I-11 contract.
     */
    @Test
    void testDirectDatabaseWrite_ForbiddenByI9_Rejected() {
        mockMvc.perform(post("/api/v1/earning/db/point-transactions")
                        .contentType("application/json")
                        .content("{\"memberId\":\"hacker-001\",\"amount\":1000000}"))
                .andExpect(status().isNotFound());
    }
}
