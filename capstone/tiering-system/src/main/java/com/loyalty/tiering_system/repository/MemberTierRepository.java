package com.loyalty.tiering_system.repository;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberTierRepository extends JpaRepository<MemberTier, UUID> {

    Optional<MemberTier> findByMemberIdAndProgramId(String memberId, String programId);

    /**
     * Dùng Pessimistic Lock khi cập nhật tier để tránh race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mt FROM MemberTier mt WHERE mt.memberId = :memberId AND mt.programId = :programId")
    Optional<MemberTier> findByMemberIdAndProgramIdForUpdate(
            @Param("memberId") String memberId,
            @Param("programId") String programId
    );

    /**
     * Lấy tất cả member đang trong grace period đã hết hạn — dùng cho batch job.
     */
    @Query("SELECT mt FROM MemberTier mt WHERE mt.status = :status AND mt.gracePeriodEnd <= :now")
    List<MemberTier> findExpiredGracePeriods(@Param("status") TierStatus status, @Param("now") LocalDateTime now);
}
