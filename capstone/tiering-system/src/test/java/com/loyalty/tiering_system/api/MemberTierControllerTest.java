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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller-layer tests for MemberTierController — UC-LB-03 (CT-12).
 *
 * Asserts HTTP status codes and response bodies against the OpenAPI contract.
 * SUT: Tiering System Service
 */
@WebMvcTest(MemberTierController.class)
class MemberTierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberTierRepository memberTierRepository;

    @Test
    void testGetMemberTier_ExistingMember_Returns200() throws Exception {
        MemberTier tier = MemberTier.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.GOLD)
                .previousTier(TierName.SILVER)
                .cumulativeQp(1500L)
                .status(TierStatus.ACTIVE)
                .effectiveFrom(LocalDateTime.now().minusMonths(1))
                .tierPeriodEnd(LocalDate.now().plusMonths(11))
                .build();

        when(memberTierRepository.findByMemberIdAndProgramId(eq("member-001"), eq("DEFAULT_PROG")))
                .thenReturn(Optional.of(tier));

        mockMvc.perform(get("/api/v1/tiering/members/member-001/tier")
                        .param("programId", "DEFAULT_PROG")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("member-001"))
                .andExpect(jsonPath("$.currentTier").value("GOLD"))
                .andExpect(jsonPath("$.previousTier").value("SILVER"))
                .andExpect(jsonPath("$.cumulativeQp").value(1500))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetMemberTier_NewMember_ReturnsDefaultSilver200() throws Exception {
        when(memberTierRepository.findByMemberIdAndProgramId(eq("member-new"), eq("DEFAULT_PROG")))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tiering/members/member-new/tier")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("member-new"))
                .andExpect(jsonPath("$.currentTier").value("SILVER"))
                .andExpect(jsonPath("$.cumulativeQp").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
