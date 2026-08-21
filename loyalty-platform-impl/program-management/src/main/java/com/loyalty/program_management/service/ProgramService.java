package com.loyalty.program_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.program_management.domain.LoyaltyProgram;
import com.loyalty.program_management.domain.ProgramStatus;
import com.loyalty.program_management.repository.LoyaltyProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Program Lifecycle Service (DD-04).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

    private final LoyaltyProgramRepository programRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoyaltyProgram createProgram(LoyaltyProgram program, UUID operatorId) {
        program.setStatus(ProgramStatus.DRAFT);
        if (program.getStartDate() == null) {
            program.setStartDate(LocalDate.now());
        }
        LoyaltyProgram saved = programRepository.save(program);

        auditLogService.logConfigChange("loyalty_program", saved.getProgramId(), operatorId,
                null, toJson(saved));
        log.info("[Program] Created program: {} (ID: {})", saved.getName(), saved.getProgramId());
        return saved;
    }

    @Transactional
    public LoyaltyProgram activateProgram(UUID programId, UUID operatorId) {
        LoyaltyProgram program = getProgramOrThrow(programId);
        if (program.getStatus() != ProgramStatus.DRAFT && program.getStatus() != ProgramStatus.SUSPENDED) {
            throw new IllegalStateException("Program must be DRAFT or SUSPENDED to activate");
        }
        // Constraints & Assumptions: A program must have at least one active EarnRule (Simulated here)

        String previousValue = toJson(program);
        program.setStatus(ProgramStatus.ACTIVE);
        LoyaltyProgram updated = programRepository.save(program);

        auditLogService.logConfigChange("loyalty_program", programId, operatorId, previousValue, toJson(updated));
        log.info("[Program] Activated program: {}", programId);
        return updated;
    }

    public List<LoyaltyProgram> getAllPrograms() {
        return programRepository.findAll();
    }

    private LoyaltyProgram getProgramOrThrow(UUID programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }
}
