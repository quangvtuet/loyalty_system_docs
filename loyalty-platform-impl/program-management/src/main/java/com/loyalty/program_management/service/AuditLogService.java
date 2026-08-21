package com.loyalty.program_management.service;

import com.loyalty.program_management.domain.ConfigVersionLog;
import com.loyalty.program_management.repository.ConfigVersionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final ConfigVersionLogRepository configVersionLogRepository;

    @Transactional
    public void logConfigChange(String entityName, UUID entityId, UUID operatorId, String previousValue, String newValue) {
        ConfigVersionLog logEntry = ConfigVersionLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .operatorId(operatorId)
                .previousValue(previousValue)
                .newValue(newValue)
                .build();
        configVersionLogRepository.save(logEntry);
        log.info("[Audit] Configuration changed for {} id: {}", entityName, entityId);
    }
}
