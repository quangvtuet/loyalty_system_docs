package com.loyalty.redemption_engine.repository;

import com.loyalty.redemption_engine.domain.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardItemRepository extends JpaRepository<RewardItem, UUID> {

    Optional<RewardItem> findByItemIdAndStatus(UUID itemId, String status);

    /**
     * Lấy catalog active, lọc theo tier eligibility (FR-03-002, FR-03-005).
     * Member chỉ thấy item có minTierRequired <= memberTier.
     */
    @Query("SELECT r FROM RewardItem r WHERE r.status = 'ACTIVE' AND r.programId = :programId " +
           "AND r.minTierRequired IN :eligibleTiers ORDER BY r.pointsCost ASC")
    List<RewardItem> findActiveCatalogForTier(@Param("programId") String programId,
                                               @Param("eligibleTiers") List<String> eligibleTiers);
}
