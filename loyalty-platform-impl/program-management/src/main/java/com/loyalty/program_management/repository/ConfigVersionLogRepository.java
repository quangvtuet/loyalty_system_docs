package com.loyalty.program_management.repository;

import com.loyalty.program_management.domain.ConfigVersionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConfigVersionLogRepository extends JpaRepository<ConfigVersionLog, UUID> {
}
