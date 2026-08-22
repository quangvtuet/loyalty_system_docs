package com.loyalty.capstone;

import com.loyalty.capstone.broker.MessageBroker;
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

    public static final String PROGRAM_ID = SimulatedConfiguration.PROGRAM_ID;
    public static final String REWARD_DIGITAL_VOUCHER = SimulatedConfiguration.REWARD_DIGITAL_VOUCHER;
    public static final String REWARD_PLATINUM_LOUNGE = SimulatedConfiguration.REWARD_PLATINUM_LOUNGE;
    public static final String REWARD_OUT_OF_STOCK = SimulatedConfiguration.REWARD_OUT_OF_STOCK;

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

    /**
     * Loads the starting configuration through the owning containers.
     * The values live in {@link SimulatedConfiguration}, not in this composition root.
     */
    private void seed() {
        SimulatedConfiguration.loadInto(programManagementService, redemptionEngineService, partnerSystems);
    }

}
