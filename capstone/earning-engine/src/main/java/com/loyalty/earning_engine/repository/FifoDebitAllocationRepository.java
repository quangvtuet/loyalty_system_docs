package com.loyalty.earning_engine.repository;

import com.loyalty.earning_engine.domain.FifoDebitAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FifoDebitAllocationRepository extends JpaRepository<FifoDebitAllocation, UUID> {
    List<FifoDebitAllocation> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
