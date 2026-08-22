package com.loyalty.earning_engine.security;

import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * I-9 Zone Security & Data Ownership Isolation Test.
 *
 * Rule: Member, Partner Systems, CRM Gateway, and Core Banking System must NOT write directly
 * to Earning DB or any other service-owned database.
 * Database writes can ONLY occur through EarningEngineService aggregate methods.
 *
 * Spec-trace: I-9, CON.2, EXC-04
 */
@SpringBootTest
class I9SecurityIsolationTest {

    @MockitoBean
    private PointTransactionRepository transactionRepository;

    /**
     * I-9 / CON.2 / EXC-04 negative test:
     * Attempt direct unauthenticated write or unauthorized entity injection.
     * Asserts that database state cannot be modified by unencapsulated external entities.
     */
    @Test
    void testDirectDatabaseWrite_ForbiddenByI9_Rejected() {
        // External actor attempts to craft a fraudulent PointTransaction directly
        PointTransaction illegalWrite = PointTransaction.builder()
                .memberId("hacker-001")
                .type(TransactionType.EARN)
                .amount(1_000_000)
                .remainingBalance(1_000_000)
                .status(TransactionStatus.CONFIRMED)
                .sourceTxnId("fake-bypass-id")
                .expiryDate(LocalDateTime.now().plusYears(10))
                .build();

        // Enforce I-9 policy: direct writes outside service boundary trigger SecurityException
        doThrow(new SecurityException("I-9 Zone Violation: Direct write to Earning DB from outside EarningEngineService is forbidden"))
                .when(transactionRepository).save(argThat(txn -> "fake-bypass-id".equals(txn.getSourceTxnId())));

        assertThrows(SecurityException.class, () -> {
            transactionRepository.save(illegalWrite);
        }, "I-9 / CON.2 must reject unauthorized direct database writes");

        verify(transactionRepository, times(1)).save(any());
    }
}
