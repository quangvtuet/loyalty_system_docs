package com.loyalty.redemption_engine.service;

import com.loyalty.redemption_engine.domain.FulfillmentType;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.repository.RewardItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    @Mock
    private RewardItemRepository rewardItemRepository;

    @InjectMocks
    private CatalogService catalogService;

    private RewardItem platinumItem;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemId = UUID.randomUUID();
        platinumItem = RewardItem.builder()
                .itemId(itemId)
                .name("Platinum Lounge Pass")
                .pointsCost(5000L)
                .minTierRequired("PLATINUM")
                .status("ACTIVE")
                .build();
    }

    @Test
    void testValidateTierEligibility_Silver_CannotAccessPlatinum() {
        // FLOW-09: Silver member tries Platinum item
        assertThrows(IllegalStateException.class, () ->
                catalogService.validateTierEligibility(platinumItem, "SILVER"));
    }

    @Test
    void testValidateTierEligibility_Gold_CannotAccessPlatinum() {
        assertThrows(IllegalStateException.class, () ->
                catalogService.validateTierEligibility(platinumItem, "GOLD"));
    }

    @Test
    void testValidateTierEligibility_Platinum_CanAccessPlatinum() {
        assertDoesNotThrow(() ->
                catalogService.validateTierEligibility(platinumItem, "PLATINUM"));
    }

    @Test
    void testValidateTierEligibility_Silver_CanAccessSilverItem() {
        RewardItem silverItem = RewardItem.builder()
                .minTierRequired("SILVER")
                .build();
        assertDoesNotThrow(() ->
                catalogService.validateTierEligibility(silverItem, "SILVER"));
    }

    @Test
    void testValidateTierEligibility_Gold_CanAccessSilverItem() {
        RewardItem silverItem = RewardItem.builder()
                .minTierRequired("SILVER")
                .build();
        // Gold thấy SILVER + GOLD items
        assertDoesNotThrow(() ->
                catalogService.validateTierEligibility(silverItem, "GOLD"));
    }

    @Test
    void testGetActiveItemOrThrow_NotFound_Throws() {
        when(rewardItemRepository.findByItemIdAndStatus(itemId, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                catalogService.getActiveItemOrThrow(itemId));
    }

    @Test
    void testGetActiveItemOrThrow_Found_ReturnsItem() {
        when(rewardItemRepository.findByItemIdAndStatus(itemId, "ACTIVE"))
                .thenReturn(Optional.of(platinumItem));

        RewardItem result = catalogService.getActiveItemOrThrow(itemId);
        assertEquals(itemId, result.getItemId());
    }
}
