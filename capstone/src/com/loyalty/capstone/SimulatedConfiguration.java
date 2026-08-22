package com.loyalty.capstone;

import com.loyalty.capstone.domain.Campaign;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.RewardItem;
import com.loyalty.capstone.external.PartnerSystemsMock;
import com.loyalty.capstone.service.ProgramManagementService;
import com.loyalty.capstone.service.redemption.RedemptionEngineService;

/**
 * The simulated configuration this slice starts from.
 *
 * <p>Program Admin operations are N/A in the I-11 slice, so there is no route that creates a
 * programme, a campaign, or a catalogue entry. This class stands in for the admin who would
 * otherwise have entered them: it holds the starting values in one named place and loads them
 * <b>through each owning container's own API</b>. It writes nothing itself, so I-7 ownership is
 * untouched — {@code Program Management Service} still writes {@code Program Mgmt DB} and
 * {@code Redemption Engine Service} still writes {@code Redemption DB}.
 *
 * <p>Rule values that Lab 2 fixed are quoted here with their requirement id. Values Lab 1 did not
 * fix are marked ASSUMPTION and are listed in {@code name-identity-map.md} section 9.
 */
final class SimulatedConfiguration {

    /** ASSUMPTION — Lab 1 names the product but gives no identifier. */
    static final String PROGRAM_ID = "BANK-REWARDS";

    /** ASSUMPTION — Lab 2 REQ-LB-06 fixes the 2x default; the identifier is invented. */
    static final String CAMPAIGN_ID = "CMP-DOUBLE-POINTS";

    /** ASSUMPTION — I-4 says Redemption DB stores reward items; no catalogue was specified. */
    static final String REWARD_DIGITAL_VOUCHER = "RI-VOUCHER-300";
    static final String REWARD_PLATINUM_LOUNGE = "RI-LOUNGE-500";
    static final String REWARD_OUT_OF_STOCK = "RI-OUTOFSTOCK-300";

    // Rule values fixed by Lab 2. These are not assumptions.
    private static final double EARN_RATE_POINTS_PER_UNIT = 1.0d;      // REQ-LB-05
    private static final long GOLD_THRESHOLD_QUALIFYING_POINTS = 1000L; // REQ-LB-18
    private static final long PLATINUM_THRESHOLD_QUALIFYING_POINTS = 3000L; // REQ-LB-18
    private static final long MINIMUM_REDEMPTION_POINTS = 100L;         // REQ-LB-27
    private static final double COST_PER_POINT_USD = 0.01d;             // REQ-LB-54
    private static final int POINT_LIFETIME_DAYS = 365;                 // REQ-LB-10
    private static final double CAMPAIGN_MULTIPLIER = 2.0d;             // REQ-LB-06
    private static final int CAMPAIGN_PRIORITY = 1;                     // REQ-LB-07

    private SimulatedConfiguration() { }

    /**
     * Loads the starting configuration through the owning containers.
     * Called once by {@link Platform}; there is no runtime path that reaches it.
     */
    static void loadInto(ProgramManagementService programManagementService,
                         RedemptionEngineService redemptionEngineService,
                         PartnerSystemsMock partnerSystems) {

        programManagementService.saveProgram(new LoyaltyProgram(
                PROGRAM_ID,
                EARN_RATE_POINTS_PER_UNIT,
                GOLD_THRESHOLD_QUALIFYING_POINTS,
                PLATINUM_THRESHOLD_QUALIFYING_POINTS,
                MINIMUM_REDEMPTION_POINTS,
                COST_PER_POINT_USD,
                POINT_LIFETIME_DAYS));

        programManagementService.saveCampaign(new Campaign(
                CAMPAIGN_ID, PROGRAM_ID, CAMPAIGN_MULTIPLIER, CAMPAIGN_PRIORITY, true));

        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_DIGITAL_VOUCHER, "Digital Voucher", 300L, "SILVER"));
        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_PLATINUM_LOUNGE, "Lounge Pass", 500L, "PLATINUM"));
        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_OUT_OF_STOCK, "Out Of Stock Voucher", 300L, "SILVER"));

        // The fake partner refuses this one item, so the CON.3 alternate can be demonstrated
        // without a flag on the public API.
        partnerSystems.makeRewardItemFail(REWARD_OUT_OF_STOCK);
    }
}
