package com.loyalty.tiering_system.service;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.domain.TierName;
import com.loyalty.tiering_system.domain.TierStatus;
import com.loyalty.tiering_system.event.TierChangedEvent;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TierUpgradeService — UC-LB-03 Apply tier upgrade.
 *
 * Spec-trace:
 * - testEvaluateAndUpgrade_HappyPath_TierUpgraded              → UC-LB-03 happy path, I-11
 * - testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice     → G6-A04, I-11, I-5 CON.1 (EXC-03)
 * - testEvaluateAndUpgrade_AlreadyAtHighestTier_NoUpgrade      → I-11 boundary
 * - testEvaluateAndUpgrade_NewMember_CreatesDefaultTier        → I-11 new-member path
 * - testEvaluateAndUpgrade_GraceRescue_TierMaintained          → I-11 grace period scenario
 */
class TierUpgradeIdempotencyTest {

    @Mock
    private MemberTierRepository memberTierRepository;

    @Mock
    private KafkaTemplate<String, TierChangedEvent> kafkaTemplate;

    @InjectMocks
    private TierUpgradeService tierUpgradeService;

    private static final String MEMBER_ID = "member-tier-test-001";
    private static final String PROGRAM_ID = "DEFAULT_PROG";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * UC-LB-03 happy path — QP accrual causes tier upgrade.
     * Member crosses the GOLD threshold (5000 QP) → MemberTier updated to GOLD.
     * Tier change event published to Message Broker (CT-07).
     * Spec-trace: I-11
     */
    @Test
    void testEvaluateAndUpgrade_HappyPath_TierUpgraded() {
        // Arrange: member is currently SILVER (0 QP threshold), about to cross GOLD threshold (1000 QP)
        MemberTier silverTier = buildMemberTier(TierName.SILVER, TierStatus.ACTIVE, 999L);
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate(MEMBER_ID, PROGRAM_ID))
                .thenReturn(Optional.of(silverTier));
        when(memberTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: cumulative QP = 1000 — crosses GOLD threshold (TierName.GOLD.getQpThreshold() == 1000)
        tierUpgradeService.evaluateAndUpgrade(MEMBER_ID, PROGRAM_ID, 1000L);

        // Assert: MemberTier upgraded to GOLD
        ArgumentCaptor<MemberTier> tierCaptor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(tierCaptor.capture());

        MemberTier saved = tierCaptor.getValue();
        assertEquals(TierName.GOLD, saved.getCurrentTier(),
                "UC-LB-03: member must be upgraded to GOLD when cumulative QP reaches 1000 threshold");
        assertEquals(TierName.SILVER, saved.getPreviousTier(),
                "Previous tier must be preserved");
        assertEquals(TierStatus.ACTIVE, saved.getStatus());

        // Verify tier change event published (CT-07 tiering.tier_changed → Message Broker)
        verify(kafkaTemplate).send(eq("loyalty.tiering.tier_changed"), eq(MEMBER_ID), any(TierChangedEvent.class));
    }

    /**
     * UC-LB-03 alt — replayed QP event idempotency (EXC-03 / CON.1 / G6-A04).
     * The same qualifying accrual event arrives twice.
     * On the second delivery, MemberTier must NOT be moved again — idempotent handling.
     * Spec-trace: G6-A04, I-11, I-5 CON.1
     */
    @Test
    void testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice() {
        // Arrange: member was already upgraded to GOLD (first delivery processed successfully)
        MemberTier goldTier = buildMemberTier(TierName.GOLD, TierStatus.ACTIVE, 1000L);
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate(MEMBER_ID, PROGRAM_ID))
                .thenReturn(Optional.of(goldTier));
        when(memberTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: same QP event replayed — cumulative still 1000 (no additional QP accrued)
        tierUpgradeService.evaluateAndUpgrade(MEMBER_ID, PROGRAM_ID, 1000L);

        // Assert: no upgrade event published — tier did not move again (EXC-03 / CON.1)
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(TierChangedEvent.class));

        // MemberTier saved but still GOLD (only cumulative QP updated)
        ArgumentCaptor<MemberTier> tierCaptor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(tierCaptor.capture());
        assertEquals(TierName.GOLD, tierCaptor.getValue().getCurrentTier(),
                "EXC-03 / CON.1: replayed event must not move MemberTier a second time");
    }

    /**
     * Boundary — member already at PLATINUM (highest tier).
     * No upgrade possible; no event published.
     * Spec-trace: I-11 boundary
     */
    @Test
    void testEvaluateAndUpgrade_AlreadyAtHighestTier_NoUpgrade() {
        MemberTier platinumTier = buildMemberTier(TierName.PLATINUM, TierStatus.ACTIVE, 3000L);
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate(MEMBER_ID, PROGRAM_ID))
                .thenReturn(Optional.of(platinumTier));
        when(memberTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tierUpgradeService.evaluateAndUpgrade(MEMBER_ID, PROGRAM_ID, 5000L);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(TierChangedEvent.class));
        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.PLATINUM, captor.getValue().getCurrentTier(),
                "Member already at PLATINUM — no upgrade should occur");
    }

    /**
     * New member path — no existing MemberTier record.
     * Service creates a default SILVER tier and evaluates upgrade.
     * Spec-trace: I-11 new-member boundary
     */
    @Test
    void testEvaluateAndUpgrade_NewMember_CreatesDefaultTier() {
        // First save: creates the default SILVER tier
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate(MEMBER_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());
        when(memberTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: first QP event for a new member (200 QP — below GOLD threshold)
        tierUpgradeService.evaluateAndUpgrade(MEMBER_ID, PROGRAM_ID, 200L);

        // Verify a MemberTier was created and saved
        verify(memberTierRepository, atLeast(1)).save(any(MemberTier.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(TierChangedEvent.class));
    }

    /**
     * Grace period rescue — member in grace period accumulates enough QP to be rescued.
     * Status transitions from IN_GRACE_PERIOD → ACTIVE, rescue event published.
     * Spec-trace: I-11 grace scenario
     */
    @Test
    void testEvaluateAndUpgrade_GraceRescue_TierMaintained() {
        // Arrange: member is GOLD but in grace period, QP currently 800 (below GOLD threshold 1000)
        MemberTier graceTier = buildMemberTier(TierName.GOLD, TierStatus.IN_GRACE_PERIOD, 800L);
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate(MEMBER_ID, PROGRAM_ID))
                .thenReturn(Optional.of(graceTier));
        when(memberTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: new QP brings cumulative to 1000 — member is rescued (GOLD threshold = 1000 QP)
        tierUpgradeService.evaluateAndUpgrade(MEMBER_ID, PROGRAM_ID, 1000L);

        // Assert: status becomes ACTIVE (grace rescue)
        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierStatus.ACTIVE, captor.getValue().getStatus(),
                "Grace rescue: status must become ACTIVE when QP reaches maintenance threshold");

        // Grace rescue event published
        verify(kafkaTemplate).send(eq("loyalty.tiering.tier_changed"), eq(MEMBER_ID),
                argThat(evt -> "GRACE_RESCUED".equals(evt.getReason())));
    }

    // ── Helper ───────────────────────────────────────────────────────

    private MemberTier buildMemberTier(TierName tier, TierStatus status, long cumulativeQp) {
        return MemberTier.builder()
                .memberId(MEMBER_ID)
                .programId(PROGRAM_ID)
                .currentTier(tier)
                .previousTier(tier == TierName.SILVER ? null : TierName.SILVER)
                .effectiveFrom(LocalDateTime.now().minusDays(30))
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .gracePeriodEnd(status == TierStatus.IN_GRACE_PERIOD
                        ? LocalDateTime.now().plusDays(20)
                        : null)
                .status(status)
                .cumulativeQp(cumulativeQp)
                .build();
    }
}
