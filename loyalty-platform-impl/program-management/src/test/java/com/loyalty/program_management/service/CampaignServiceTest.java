package com.loyalty.program_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.program_management.domain.Campaign;
import com.loyalty.program_management.domain.CampaignStatus;
import com.loyalty.program_management.repository.CampaignRepository;
import com.loyalty.program_management.repository.ConfigVersionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetWinningCampaign_MultipleActive_ReturnsHighestPriority() {
        UUID programId = UUID.randomUUID();
        
        // Priority 1 is higher priority than Priority 5
        Campaign highPriority = Campaign.builder().name("High Priority").priority(1).build();
        Campaign lowPriority = Campaign.builder().name("Low Priority").priority(5).build();

        // Repository should return them ordered by priority ASC
        when(campaignRepository.findActiveCampaignsForDate(eq(programId), any()))
                .thenReturn(List.of(highPriority, lowPriority));

        Campaign winner = campaignService.getWinningCampaign(programId);

        assertNotNull(winner);
        assertEquals("High Priority", winner.getName());
    }

    @Test
    void testDeactivateExpiredCampaigns_UpdatesStatus() {
        Campaign expired = Campaign.builder()
                .campaignId(UUID.randomUUID())
                .status(CampaignStatus.ACTIVE)
                .build();

        when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.ACTIVE), any()))
                .thenReturn(List.of(expired));

        campaignService.deactivateExpiredCampaigns();

        verify(campaignRepository).save(expired);
        assertEquals(CampaignStatus.EXPIRED, expired.getStatus());
        verify(auditLogService).logConfigChange(eq("campaign"), any(), any(), any(), any());
    }
}
