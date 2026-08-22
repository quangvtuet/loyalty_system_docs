package com.loyalty.earning_engine.repository;

import com.loyalty.earning_engine.domain.PointBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointBalanceRepository extends JpaRepository<PointBalance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM PointBalance pb WHERE pb.memberId = :memberId AND pb.programId = :programId")
    Optional<PointBalance> findByMemberIdAndProgramIdForUpdate(@Param("memberId") String memberId, @Param("programId") String programId);
    
    Optional<PointBalance> findByMemberIdAndProgramId(String memberId, String programId);
}
