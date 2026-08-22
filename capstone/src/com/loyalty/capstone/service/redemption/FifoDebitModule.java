package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.service.EarningEngineService;

import java.util.List;

/**
 * Lab 9 Component 'FIFO Debit Module'.
 * It asks Earning Engine Service for the debit; it never writes Earning DB itself (CON.2).
 */
public final class FifoDebitModule {

    private final EarningEngineService earningEngineService;

    public FifoDebitModule(EarningEngineService earningEngineService) {
        this.earningEngineService = earningEngineService;
    }

    public List<DebitAllocation> reserve(String memberId, long pointsCost) {
        return earningEngineService.debitFifo(memberId, pointsCost);
    }

    public void restore(List<DebitAllocation> allocations) {
        earningEngineService.restore(allocations);
    }
}
