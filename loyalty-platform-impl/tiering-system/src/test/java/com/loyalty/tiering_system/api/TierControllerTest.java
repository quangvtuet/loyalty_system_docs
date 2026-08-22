package com.loyalty.tiering_system.api;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierName;
import com.loyalty.tiering_system.domain.TierStatus;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller-layer tests for TierController — CT-12 GetMemberTier.
 * OpenAPI operationId: getMemberTier
 * Path: GET /api/v1/tiering/members/{memberId}/tier
 */
@WebMvcTest(TierController.class)
class TierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberTierRepository memberTierRepository;

    @Test
    void testGetMemberTier_Found_Returns200() throws Exception {
        MemberTier tier = MemberTier.builder()
                .memberTierId(UUID.randomUUID())
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.GOLD)
                .previousTier(TierName.SILVER)
                .effectiveFrom(LocalDateTime.now().minusDays(10))
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .status(TierStatus.ACTIVE)
                .cumulativeQp(1200L)
                .build();

        when(memberTierRepository.findByMemberIdAndProgramId(eq("member-001"), eq("DEFAULT_PROG")))
                .thenReturn(Optional.of(tier));

        mockMvc.perform(get("/api/v1/tiering/members/member-001/tier")
                        .param("programId", "DEFAULT_PROG")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("member-001"))
                .andExpect(jsonPath("$.currentTier").value("GOLD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cumulativeQp").value(1200));
    }

    @Test
    void testGetMemberTier_NotFound_Returns404() throws Exception {
        when(memberTierRepository.findByMemberIdAndProgramId(eq("unknown-member"), eq("DEFAULT_PROG")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tiering/members/unknown-member/tier")
                        .param("programId", "DEFAULT_PROG")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
