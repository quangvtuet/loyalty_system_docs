package com.loyalty.earning_engine.repository;

import com.loyalty.earning_engine.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {
    Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);
}
