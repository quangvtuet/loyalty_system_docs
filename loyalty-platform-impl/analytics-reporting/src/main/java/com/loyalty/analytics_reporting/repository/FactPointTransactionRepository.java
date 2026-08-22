package com.loyalty.analytics_reporting.repository;

import com.loyalty.analytics_reporting.domain.FactPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface FactPointTransactionRepository extends JpaRepository<FactPointTransaction, Long> {

    @Query("SELECT SUM(f.financialLiabilityUsd) FROM FactPointTransaction f JOIN DimProgram p ON f.programKey = p.programKey " +
           "WHERE p.programId = :programId AND f.pointsRemaining > 0 AND f.expiryDate > :now")
    BigDecimal calculateTotalLiability(@Param("programId") UUID programId, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(f.pointsRemaining) FROM FactPointTransaction f JOIN DimProgram p ON f.programKey = p.programKey " +
           "WHERE p.programId = :programId AND f.pointsRemaining > 0 AND f.expiryDate > :now " +
           "AND f.earnDate >= :fromDate AND f.earnDate < :toDate")
    Long calculateUnspentPointsInAgeBucket(@Param("programId") UUID programId,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate,
                                           @Param("now") LocalDateTime now);

    /**
     * CON.4 — Returns the most recent earnDate in the Data Warehouse for the given program.
     * Used by ReportingService to determine data freshness (staleness > 10 min → EXC-07).
     */
    @Query("SELECT MAX(f.earnDate) FROM FactPointTransaction f JOIN DimProgram p ON f.programKey = p.programKey " +
           "WHERE p.programId = :programId")
    LocalDateTime findMaxEarnDate(@Param("programId") UUID programId);
}

