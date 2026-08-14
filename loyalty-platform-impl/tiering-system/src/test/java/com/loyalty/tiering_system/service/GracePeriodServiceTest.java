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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GracePeriodServiceTest {

    @Mock
    private MemberTierRepository memberTierRepository;

    @Mock
    private KafkaTemplate<String, TierChangedEvent> kafkaTemplate;

    @InjectMocks
    private GracePeriodService gracePeriodService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitiateGracePeriod_SetsStatusAndGraceEnd() {
        MemberTier member = MemberTier.builder()
                .memberId("member-platinum")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.PLATINUM)
                .status(TierStatus.ACTIVE)
                .cumulativeQp(1500L)
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .effectiveFrom(LocalDateTime.now().minusMonths(6))
                .build();

        when(memberTierRepository.save(any())).thenReturn(member);

        gracePeriodService.initiateGracePeriod(member);

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierStatus.IN_GRACE_PERIOD, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getGracePeriodEnd());
        // Grace period phải ~30 ngày từ bây giờ
        assertTrue(captor.getValue().getGracePeriodEnd().isAfter(LocalDateTime.now().plusDays(29)));
        assertTrue(captor.getValue().getGracePeriodEnd().isBefore(LocalDateTime.now().plusDays(31)));
    }

    @Test
    void testProcessExpiredGracePeriods_Platinum_DowngradesToGold() {
        // UC-02-04: Grace period hết hạn, Platinum -> Gold (1 bậc)
        MemberTier expiredMember = MemberTier.builder()
                .memberId("member-expired")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.PLATINUM)
                .previousTier(TierName.PLATINUM)
                .status(TierStatus.IN_GRACE_PERIOD)
                .gracePeriodEnd(LocalDateTime.now().minusHours(1)) // đã hết hạn
                .cumulativeQp(1500L)
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .effectiveFrom(LocalDateTime.now().minusYears(1))
                .build();

        when(memberTierRepository.findExpiredGracePeriods(eq(TierStatus.IN_GRACE_PERIOD), any()))
                .thenReturn(List.of(expiredMember));
        when(memberTierRepository.save(any())).thenReturn(expiredMember);

        gracePeriodService.processExpiredGracePeriods();

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.GOLD, captor.getValue().getCurrentTier()); // Platinum -> Gold (1 bậc)
        assertEquals(TierName.PLATINUM, captor.getValue().getPreviousTier());
        assertEquals(TierStatus.DOWNGRADED, captor.getValue().getStatus());
        assertNull(captor.getValue().getGracePeriodEnd());

        // Verify Kafka event
        ArgumentCaptor<TierChangedEvent> eventCaptor = ArgumentCaptor.forClass(TierChangedEvent.class);
        verify(kafkaTemplate).send(any(), eq("member-expired"), eventCaptor.capture());
        assertEquals("DOWNGRADED", eventCaptor.getValue().getReason());
        assertEquals("PLATINUM", eventCaptor.getValue().getPreviousTier());
        assertEquals("GOLD", eventCaptor.getValue().getNewTier());
    }

    @Test
    void testProcessExpiredGracePeriods_Gold_DowngradesToSilver() {
        // FR-02-023: Gold -> Silver (không bỏ qua tier)
        MemberTier expiredGold = MemberTier.builder()
                .memberId("member-gold-expired")
                .programId("DEFAULT_PROG")
                .currentTier(TierName.GOLD)
                .status(TierStatus.IN_GRACE_PERIOD)
                .gracePeriodEnd(LocalDateTime.now().minusDays(1))
                .cumulativeQp(500L)
                .tierPeriodEnd(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .effectiveFrom(LocalDateTime.now().minusYears(1))
                .build();

        when(memberTierRepository.findExpiredGracePeriods(eq(TierStatus.IN_GRACE_PERIOD), any()))
                .thenReturn(List.of(expiredGold));
        when(memberTierRepository.save(any())).thenReturn(expiredGold);

        gracePeriodService.processExpiredGracePeriods();

        ArgumentCaptor<MemberTier> captor = ArgumentCaptor.forClass(MemberTier.class);
        verify(memberTierRepository).save(captor.capture());
        assertEquals(TierName.SILVER, captor.getValue().getCurrentTier()); // Gold -> Silver
    }

    @Test
    void testProcessExpiredGracePeriods_NoExpired_DoesNothing() {
        when(memberTierRepository.findExpiredGracePeriods(eq(TierStatus.IN_GRACE_PERIOD), any()))
                .thenReturn(List.of());

        gracePeriodService.processExpiredGracePeriods();

        verify(memberTierRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
