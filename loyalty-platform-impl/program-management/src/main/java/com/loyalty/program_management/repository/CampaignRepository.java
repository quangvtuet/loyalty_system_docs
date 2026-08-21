package com.loyalty.program_management.repository;

import com.loyalty.program_management.domain.Campaign;
import com.loyalty.program_management.domain.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    @Query("SELECT c FROM Campaign c WHERE c.programId = :programId AND c.status = 'ACTIVE' " +
           "AND c.startDate <= :now AND c.endDate >= :now ORDER BY c.priority ASC")
    List<Campaign> findActiveCampaignsForDate(@Param("programId") UUID programId, @Param("now") LocalDateTime now);

    List<Campaign> findByStatusAndEndDateBefore(CampaignStatus status, LocalDateTime endDate);
}
