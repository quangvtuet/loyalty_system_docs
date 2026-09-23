package com.loyalty.capstone.service;

import com.loyalty.capstone.domain.Campaign;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.store.ProgramMgmtDb;

import java.util.Comparator;
import java.util.List;

/** I-4 container 'Program Management Service'. Sole writer of Program Mgmt DB. */
public final class ProgramManagementService {

    public static final String CONTAINER = "Program Management Service";

    private final ProgramMgmtDb programMgmtDb;

    public ProgramManagementService(ProgramMgmtDb programMgmtDb) {
        this.programMgmtDb = programMgmtDb;
    }

    public void saveProgram(LoyaltyProgram program) {
        programMgmtDb.saveProgram(CONTAINER, program);
    }

    public void saveCampaign(Campaign campaign) {
        programMgmtDb.saveCampaign(CONTAINER, campaign);
    }

    public LoyaltyProgram program(String programId) {
        return programMgmtDb.program(programId);
    }

    /** Lowest priority number wins when several campaigns are active. */
    public Campaign winningCampaign(String programId) {
        List<Campaign> active = programMgmtDb.activeCampaigns(programId);
        return active.stream().min(Comparator.comparingInt(c -> c.priority)).orElse(null);
    }
}
