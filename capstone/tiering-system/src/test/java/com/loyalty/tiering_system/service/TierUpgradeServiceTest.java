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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TierUpgradeServiceTest {

    @Mock
    private MemberTierRepository memberTierRepository;

    @Mock
    private KafkaTemplate<String, TierChangedEvent> kafkaTemplate;

    @InjectMocks
    private TierUpgradeService tierUpgradeService;

    private MemberTier silverMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        silverMember = MemberTier.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.SILVER)
                .previousTier(null)
                .effectiveFrom(LocalDateTime.now().minusMonths(1))
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .status(TierStatus.ACTIVE)
                .cumulativeQp(900L)
                .build();
    }

    @Test
    void testEvaluate_Silver900QP_NoUpgrade() {
        // 900 QP chưa đủ 1000 QP để lên Gold
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate("member-001", "DEFAULT_PROG"))
                .thenReturn(Optional.of(silverMember));
        when(memberTierRepository.save(any())).thenReturn(silverMember);

        tierUpgradeService.evaluateAndUpgrade("member-001", "DEFAULT_PROG", 900L);

        // Verify: vẫn là SILVER, không publish event
        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.SILVER, captor.getValue().getCurrentTier());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void testEvaluate_Silver1100QP_UpgradeToGold() {
        // UC-02-02: Silver với 900 QP nhận thêm 200 QP -> 1100 QP >= 1000 -> Upgrade to Gold
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate("member-001", "DEFAULT_PROG"))
                .thenReturn(Optional.of(silverMember));
        when(memberTierRepository.save(any())).thenReturn(silverMember);

        tierUpgradeService.evaluateAndUpgrade("member-001", "DEFAULT_PROG", 1100L);

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.GOLD, captor.getValue().getCurrentTier());
        assertEquals(TierName.SILVER, captor.getValue().getPreviousTier());
        assertEquals(TierStatus.ACTIVE, captor.getValue().getStatus());

        // Verify Kafka event published
        ArgumentCaptor<TierChangedEvent> eventCaptor = ArgumentCaptor.forClass(TierChangedEvent.class);
        verify(kafkaTemplate).send(eq("loyalty.tiering.tier_changed"), eq("member-001"), eventCaptor.capture());
        assertEquals("SILVER", eventCaptor.getValue().getPreviousTier());
        assertEquals("GOLD", eventCaptor.getValue().getNewTier());
        assertEquals("UPGRADE", eventCaptor.getValue().getReason());
        assertEquals(1.5, eventCaptor.getValue().getNewEarnMultiplier());
    }

    @Test
    void testEvaluate_Silver3500QP_UpgradeToPlatinumDirectly() {
        // FR-02-013: Multi-threshold cross — Silver lên thẳng Platinum (skip Gold) khi QP >= 3000
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate("member-001", "DEFAULT_PROG"))
                .thenReturn(Optional.of(silverMember));
        when(memberTierRepository.save(any())).thenReturn(silverMember);

        tierUpgradeService.evaluateAndUpgrade("member-001", "DEFAULT_PROG", 3500L);

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.PLATINUM, captor.getValue().getCurrentTier());
        assertEquals(TierName.SILVER, captor.getValue().getPreviousTier());
    }

    @Test
    void testEvaluate_NewMember_CreatesDefaultSilverTier() {
        // Member hoàn toàn mới, chưa có record trong DB
        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate("new-member", "DEFAULT_PROG"))
                .thenReturn(Optional.empty());
        MemberTier newTier = MemberTier.builder()
                .memberId("new-member")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.SILVER)
                .status(TierStatus.ACTIVE)
                .cumulativeQp(0L)
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .effectiveFrom(LocalDateTime.now())
                .build();
        when(memberTierRepository.save(any())).thenReturn(newTier);

        tierUpgradeService.evaluateAndUpgrade("new-member", "DEFAULT_PROG", 500L);

        verify(memberTierRepository, atLeastOnce()).save(any(MemberTier.class));
    }

    @Test
    void testEvaluate_GracePeriod_Rescued() {
        // Member đang IN_GRACE_PERIOD với Gold, kiếm đủ QP để giữ Gold
        MemberTier graceMember = MemberTier.builder()
                .memberId("member-grace")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.PLATINUM)
                .previousTier(TierName.PLATINUM)
                .effectiveFrom(LocalDateTime.now().minusMonths(1))
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .gracePeriodEnd(LocalDateTime.now().plusDays(15))
                .status(TierStatus.IN_GRACE_PERIOD)
                .cumulativeQp(1500L)
                .build();

        when(memberTierRepository.findByMemberIdAndProgramIdForUpdate("member-grace", "DEFAULT_PROG"))
                .thenReturn(Optional.of(graceMember));
        when(memberTierRepository.save(any())).thenReturn(graceMember);

        // Earn đủ QP đạt 3100 >= 3000 threshold của Platinum
        tierUpgradeService.evaluateAndUpgrade("member-grace", "DEFAULT_PROG", 3100L);

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierStatus.ACTIVE, captor.getValue().getStatus());
        assertNull(captor.getValue().getGracePeriodEnd());

        ArgumentCaptor<TierChangedEvent> eventCaptor = ArgumentCaptor.forClass(TierChangedEvent.class);
        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());
        assertEquals("GRACE_RESCUED", eventCaptor.getValue().getReason());
    }
}
