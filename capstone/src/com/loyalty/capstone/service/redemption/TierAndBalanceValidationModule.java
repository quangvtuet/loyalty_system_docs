package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.RewardItem;
import com.loyalty.capstone.service.EarningEngineService;
import com.loyalty.capstone.service.TieringSystemService;

import java.util.Arrays;
import java.util.List;

/**
 * Lab 9 Component 'Tier and Balance Validation Module'.
 * Checks minimum tier, available balance, and the minimum redemption amount before any debit.
 */
public final class TierAndBalanceValidationModule {

    private static final List<String> TIER_ORDER = Arrays.asList("SILVER", "GOLD", "PLATINUM");

    private final TieringSystemService tieringSystemService;
    private final EarningEngineService earningEngineService;

    public TierAndBalanceValidationModule(TieringSystemService tieringSystemService,
                                          EarningEngineService earningEngineService) {
        this.tieringSystemService = tieringSystemService;
        this.earningEngineService = earningEngineService;
    }

    public ValidationOutcome validate(String memberId, RewardItem rewardItem, LoyaltyProgram program) {
        if (rewardItem.pointsCost < program.minimumRedemptionPoints) {
            return ValidationOutcome.rejected("below minimum redemption of "
                    + program.minimumRedemptionPoints + " points");
        }
        String memberTier = tieringSystemService.currentTier(memberId);
        if (TIER_ORDER.indexOf(memberTier) < TIER_ORDER.indexOf(rewardItem.minimumTier)) {
            return ValidationOutcome.rejected("tier " + memberTier + " is below required "
                    + rewardItem.minimumTier);
        }
        if (earningEngineService.availablePoints(memberId) < rewardItem.pointsCost) {
            return ValidationOutcome.rejected("insufficient balance");
        }
        return ValidationOutcome.accepted();
    }
}
