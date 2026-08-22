package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.domain.RewardItem;
import com.loyalty.capstone.external.CrmNotificationGateway;
import com.loyalty.capstone.external.PartnerSystems;
import com.loyalty.capstone.service.EarningEngineService;
import com.loyalty.capstone.service.ProgramManagementService;
import com.loyalty.capstone.service.TieringSystemService;
import com.loyalty.capstone.store.IdempotencyStore;
import com.loyalty.capstone.store.RedemptionDb;

import java.util.List;

/**
 * I-4 container 'Redemption Engine Service' — the one I-11 container drilled to modules.
 * Sole writer of Redemption DB. UC-LB-02 runs here.
 */
public final class RedemptionEngineService {

    public static final String CONTAINER = "Redemption Engine Service";

    private final RedemptionDb redemptionDb;
    private final IdempotencyStore idempotencyStore;
    private final ProgramManagementService programManagementService;
    private final TieringSystemService tieringSystemService;
    private final String programId;

    private final OrderIntakeModule orderIntakeModule;
    private final TierAndBalanceValidationModule validationModule;
    private final FifoDebitModule fifoDebitModule;
    private final RedemptionOrderStateModule stateModule;
    private final FulfillmentCoordinationModule fulfillmentModule;

    public RedemptionEngineService(RedemptionDb redemptionDb, IdempotencyStore idempotencyStore,
                                   TieringSystemService tieringSystemService,
                                   EarningEngineService earningEngineService,
                                   ProgramManagementService programManagementService,
                                   PartnerSystems partnerSystems,
                                   CrmNotificationGateway crmNotificationGateway,
                                   String programId) {
        this.redemptionDb = redemptionDb;
        this.idempotencyStore = idempotencyStore;
        this.programManagementService = programManagementService;
        this.tieringSystemService = tieringSystemService;
        this.programId = programId;

        this.orderIntakeModule = new OrderIntakeModule();
        this.validationModule = new TierAndBalanceValidationModule(tieringSystemService, earningEngineService);
        this.fifoDebitModule = new FifoDebitModule(earningEngineService);
        this.stateModule = new RedemptionOrderStateModule(redemptionDb, CONTAINER);
        this.fulfillmentModule = new FulfillmentCoordinationModule(
                partnerSystems, crmNotificationGateway, fifoDebitModule, stateModule);
    }

    public void seedRewardItem(RewardItem item) {
        redemptionDb.saveRewardItem(CONTAINER, item);
    }

    public List<RewardItem> catalogue() { return redemptionDb.catalogue(); }

    public RedemptionOrder order(String orderId) { return redemptionDb.order(orderId); }

    /** UC-LB-02 happy path plus the three named alternates. */
    public RedemptionOrder submitRedemption(String memberId, String rewardItemId) {
        RewardItem rewardItem = redemptionDb.rewardItem(rewardItemId);
        if (rewardItem == null) {
            throw new IllegalArgumentException("unknown reward item " + rewardItemId);
        }
        LoyaltyProgram program = programManagementService.program(programId);

        String memberTierAtOrder = tieringSystemService.currentTier(memberId);
        RedemptionOrder order = orderIntakeModule.accept(memberId, rewardItemId,
                rewardItem.pointsCost, memberTierAtOrder);
        stateModule.createPending(order);

        if (!idempotencyStore.acquireBalanceLock(memberId)) {
            stateModule.toCancelled(order, "member balance is locked by another order");
            return order;
        }
        try {
            ValidationOutcome outcome = validationModule.validate(memberId, rewardItem, program);
            if (!outcome.accepted) {
                stateModule.toCancelled(order, outcome.rejectionReason);
                return order;
            }
            List<DebitAllocation> allocations = fifoDebitModule.reserve(memberId, rewardItem.pointsCost);
            stateModule.toInProgress(order, allocations);
        } finally {
            idempotencyStore.releaseBalanceLock(memberId);
        }

        fulfillmentModule.dispatch(order);
        return order;
    }
}
