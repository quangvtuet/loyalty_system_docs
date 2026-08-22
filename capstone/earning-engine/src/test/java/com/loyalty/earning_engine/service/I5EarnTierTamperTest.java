package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.client.TieringClient;
import com.loyalty.earning_engine.dto.EarnEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * I-5 Hard Rule & Invariant Test: Client Tier Tamper Resistance on Earn Events.
 *
 * Rule: EarnCalculator must NEVER trust client-supplied tier values in earn events.
 * The system MUST query Tiering System Service (CT-12) to obtain the authoritative MemberTier.
 *
 * Spec-trace: I-5, CON.2, CT-12, T3
 */
class I5EarnTierTamperTest {

    @Mock
    private EarningLedgerService ledgerService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private TieringClient tieringClient;

    private EarnCalculator earnCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        earnCalculator = new EarnCalculator(ledgerService, kafkaTemplate, tieringClient);
    }

    @Test
    @DisplayName("NEG-I5-02: Malicious client sends forged PLATINUM tier; CT-12 returns SILVER -> awards 1.0x base points")
    void testClientForgedTier_OverriddenByCT12_AwardsBaseMultiplierOnly() {
        when(tieringClient.getMemberTier("attacker-001", "DEFAULT_PROG")).thenReturn("SILVER");

        EarnEventResponse response = earnCalculator.processEarn(
                "attacker-001",
                1000,
                "txn-forged-tier-001",
                "PLATINUM", // forged tier in client payload
                null,
                "DEFAULT_PROG"
        );

        // Assert CT-12 was queried
        verify(tieringClient, times(1)).getMemberTier("attacker-001", "DEFAULT_PROG");
        // Base points must be calculated with SILVER multiplier (1.0x -> 1000 pts), not PLATINUM (2000 pts)
        assertNotNull(response);
        assertEquals(1000L, response.getBasePoints());
        assertEquals(1000L, response.getTotalPoints());
        verify(ledgerService, times(1)).recordBaseEarn("attacker-001", 1000, "txn-forged-tier-001");
    }

    @Test
    @DisplayName("POS-I5-02: Legitimate PLATINUM member confirmed by CT-12 receives 2.0x base multiplier")
    void testLegitimatePlatinumMember_ConfirmedByCT12_ReceivesDoublePoints() {
        when(tieringClient.getMemberTier("vip-member-001", "DEFAULT_PROG")).thenReturn("PLATINUM");

        EarnEventResponse response = earnCalculator.processEarn(
                "vip-member-001",
                1000,
                "txn-legit-001",
                "SILVER", // client omitted or sent default
                null,
                "DEFAULT_PROG"
        );

        verify(tieringClient, times(1)).getMemberTier("vip-member-001", "DEFAULT_PROG");
        // Base points must be calculated with PLATINUM multiplier (2.0x -> 2000 pts)
        assertNotNull(response);
        assertEquals(2000L, response.getBasePoints());
        assertEquals(2000L, response.getTotalPoints());
        verify(ledgerService, times(1)).recordBaseEarn("vip-member-001", 2000, "txn-legit-001");
    }
}
