package com.loyalty.program_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.program_management.domain.Campaign;
import com.loyalty.program_management.domain.CampaignStatus;
import com.loyalty.program_management.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Campaign & Priority Engine (DD-04).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Campaign createCampaign(Campaign campaign, UUID operatorId) {
        campaign.setStatus(CampaignStatus.ACTIVE);
        if (campaign.getPointsIssuedTotal() == null) {
            campaign.setPointsIssuedTotal(0L);
        }
        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logConfigChange("campaign", saved.getCampaignId(), operatorId,
                null, toJson(saved));
        log.info("[Campaign] Created campaign: {} (Priority: {})", saved.getName(), saved.getPriority());
        return saved;
    }

    /**
     * Lấy các campaign đang active và tìm ra campaign có priority cao nhất (giá trị nhỏ nhất).
     * FR-04-012: Priority conflict resolution.
     */
    public Campaign getWinningCampaign(UUID programId) {
        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaignsForDate(programId, LocalDateTime.now());
        if (activeCampaigns.isEmpty()) {
            return null;
        }
        // Vì query đã ORDER BY priority ASC, phần tử đầu tiên là priority cao nhất
        Campaign winning = activeCampaigns.get(0);
        log.info("[Campaign] Winning campaign for program {}: {} (Priority: {})", programId, winning.getName(), winning.getPriority());
        return winning;
    }

    @Transactional
    public void deactivateExpiredCampaigns() {
        List<Campaign> expired = campaignRepository.findByStatusAndEndDateBefore(CampaignStatus.ACTIVE, LocalDateTime.now());
        for (Campaign c : expired) {
            String prev = toJson(c);
            c.setStatus(CampaignStatus.EXPIRED);
            campaignRepository.save(c);
            auditLogService.logConfigChange("campaign", c.getCampaignId(), UUID.fromString("00000000-0000-0000-0000-000000000000"), prev, toJson(c));
            log.info("[Campaign] Auto-deactivated expired campaign: {}", c.getCampaignId());
        }
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
