package com.loyalty.capstone;

import com.loyalty.capstone.broker.MessageBroker;
import com.loyalty.capstone.domain.Campaign;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.RewardItem;
import com.loyalty.capstone.external.CoreBankingSystemMock;
import com.loyalty.capstone.external.CrmNotificationGatewayMock;
import com.loyalty.capstone.external.EnterpriseDataWarehouseMock;
import com.loyalty.capstone.external.PartnerSystemsMock;
import com.loyalty.capstone.service.AnalyticsReportingService;
import com.loyalty.capstone.service.EarningEngineService;
import com.loyalty.capstone.service.ProgramManagementService;
import com.loyalty.capstone.service.TieringSystemService;
import com.loyalty.capstone.service.redemption.RedemptionEngineService;
import com.loyalty.capstone.store.DataWarehouse;
import com.loyalty.capstone.store.EarningDb;
import com.loyalty.capstone.store.IdempotencyStore;
import com.loyalty.capstone.store.ProgramMgmtDb;
import com.loyalty.capstone.store.RedemptionDb;
import com.loyalty.capstone.store.TieringDb;

import java.time.Clock;

/**
 * Composition root for the collapsed runtime.
 * One process holds every I-4 container as a module. See name-identity-map.md for the collapse rows.
 */
public final class Platform {

    public static final String PROGRAM_ID = "BANK-REWARDS";
    public static final String REWARD_DIGITAL_VOUCHER = "RI-VOUCHER-300";
    public static final String REWARD_PLATINUM_LOUNGE = "RI-LOUNGE-500";
    public static final String REWARD_OUT_OF_STOCK = "RI-OUTOFSTOCK-300";

    public final MessageBroker messageBroker;
    public final IdempotencyStore idempotencyStore;
    public final EarningDb earningDb;
    public final TieringDb tieringDb;
    public final RedemptionDb redemptionDb;
    public final ProgramMgmtDb programMgmtDb;
    public final DataWarehouse dataWarehouse;

    public final ProgramManagementService programManagementService;
    public final EarningEngineService earningEngineService;
    public final TieringSystemService tieringSystemService;
    public final RedemptionEngineService redemptionEngineService;
    public final AnalyticsReportingService analyticsReportingService;

    public final CoreBankingSystemMock coreBankingSystem;
    public final PartnerSystemsMock partnerSystems;
    public final CrmNotificationGatewayMock crmNotificationGateway;
    public final EnterpriseDataWarehouseMock enterpriseDataWarehouse;

    public Platform(Clock clock) {
        this.messageBroker = new MessageBroker();
        this.idempotencyStore = new IdempotencyStore();
        this.earningDb = new EarningDb();
        this.tieringDb = new TieringDb();
        this.redemptionDb = new RedemptionDb();
        this.programMgmtDb = new ProgramMgmtDb();
        this.dataWarehouse = new DataWarehouse();

        this.partnerSystems = new PartnerSystemsMock();
        this.crmNotificationGateway = new CrmNotificationGatewayMock();
        this.enterpriseDataWarehouse = new EnterpriseDataWarehouseMock();

        this.programManagementService = new ProgramManagementService(programMgmtDb);
        this.tieringSystemService = new TieringSystemService(tieringDb, programManagementService,
                messageBroker, PROGRAM_ID);
        this.earningEngineService = new EarningEngineService(earningDb, idempotencyStore,
                programManagementService, messageBroker, clock);
        this.redemptionEngineService = new RedemptionEngineService(redemptionDb, idempotencyStore,
                tieringSystemService, earningEngineService, programManagementService,
                partnerSystems, crmNotificationGateway, PROGRAM_ID);
        this.analyticsReportingService = new AnalyticsReportingService(dataWarehouse,
                programManagementService, crmNotificationGateway, messageBroker, clock, PROGRAM_ID);

        this.coreBankingSystem = new CoreBankingSystemMock(messageBroker);

        seed();
    }

    private void seed() {
        programManagementService.saveProgram(new LoyaltyProgram(
                PROGRAM_ID, 1.0d, 1000L, 3000L, 100L, 0.01d, 365));
        programManagementService.saveCampaign(new Campaign(
                "CMP-DOUBLE-POINTS", PROGRAM_ID, 2.0d, 1, true));

        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_DIGITAL_VOUCHER, "Digital Voucher", 300L, "SILVER"));
        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_PLATINUM_LOUNGE, "Lounge Pass", 500L, "PLATINUM"));
        redemptionEngineService.seedRewardItem(
                new RewardItem(REWARD_OUT_OF_STOCK, "Out Of Stock Voucher", 300L, "SILVER"));

        partnerSystems.makeRewardItemFail(REWARD_OUT_OF_STOCK);
    }
}
