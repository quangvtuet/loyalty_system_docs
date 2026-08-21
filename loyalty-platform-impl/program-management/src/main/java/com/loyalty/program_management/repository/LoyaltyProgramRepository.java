package com.loyalty.program_management.repository;

import com.loyalty.program_management.domain.LoyaltyProgram;
import com.loyalty.program_management.domain.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, UUID> {
    List<LoyaltyProgram> findByStatus(ProgramStatus status);
}
