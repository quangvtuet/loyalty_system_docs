package com.loyalty.analytics_reporting.repository;

import com.loyalty.analytics_reporting.domain.DimProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DimProgramRepository extends JpaRepository<DimProgram, Long> {
    Optional<DimProgram> findByProgramId(UUID programId);
}
