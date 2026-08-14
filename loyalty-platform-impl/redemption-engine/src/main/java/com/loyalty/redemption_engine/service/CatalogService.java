package com.loyalty.redemption_engine.service;

import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.repository.RewardItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Catalog Service — quản lý reward catalog, kiểm tra tier eligibility.
 * FR-03-001, FR-03-002, FR-03-003, FR-03-005.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final RewardItemRepository rewardItemRepository;

    /**
     * Lấy catalog phù hợp với tier của member (FR-03-002).
     * Silver thấy SILVER items, Gold thấy SILVER+GOLD, Platinum thấy tất cả.
     */
    public List<RewardItem> getCatalogForMember(String programId, String memberTier) {
        List<String> eligibleTiers = getEligibleTiersFor(memberTier);
        return rewardItemRepository.findActiveCatalogForTier(programId, eligibleTiers);
    }

    /**
     * Kiểm tra item có tồn tại và active không (FR-03-011).
     */
    public RewardItem getActiveItemOrThrow(UUID itemId) {
        return rewardItemRepository.findByItemIdAndStatus(itemId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reward item not found or not ACTIVE: " + itemId));
    }

    /**
     * Kiểm tra member tier có đủ điều kiện không (FR-03-011).
     */
    public void validateTierEligibility(RewardItem item, String memberTier) {
        List<String> eligible = getEligibleTiersFor(memberTier);
        if (!eligible.contains(item.getMinTierRequired())) {
            log.warn("[Tier Check] Member tier {} cannot access item requiring {}", memberTier, item.getMinTierRequired());
            throw new IllegalStateException(
                    "ERR_RED_TIER_ELIGIBILITY_FAILED: Item requires " + item.getMinTierRequired()
                    + " but member is " + memberTier);
        }
    }

    private List<String> getEligibleTiersFor(String memberTier) {
        return switch (memberTier.toUpperCase()) {
            case "PLATINUM" -> Arrays.asList("SILVER", "GOLD", "PLATINUM");
            case "GOLD"     -> Arrays.asList("SILVER", "GOLD");
            default         -> List.of("SILVER");
        };
    }
}
