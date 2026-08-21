package com.loyalty.redemption_engine.repository;

import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RedemptionOrderRepository extends JpaRepository<RedemptionOrder, UUID> {

    List<RedemptionOrder> findByMemberIdAndProgramIdOrderByCreatedAtDesc(String memberId, String programId);
}
