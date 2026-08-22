package com.loyalty.capstone.store;

import com.loyalty.capstone.domain.Campaign;
import com.loyalty.capstone.domain.LoyaltyProgram;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** I-4 container 'Program Mgmt DB'. I-7 owner of LoyaltyProgram and Campaign. */
public final class ProgramMgmtDb extends OwnedStore {

    private final Map<String, LoyaltyProgram> programs = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>();

    public ProgramMgmtDb() { super("Program Mgmt DB", "Program Management Service"); }

    public void saveProgram(String writerContainerName, LoyaltyProgram program) {
        assertWriter(writerContainerName);
        programs.put(program.programId, program);
    }

    public LoyaltyProgram program(String programId) { return programs.get(programId); }

    public void saveCampaign(String writerContainerName, Campaign campaign) {
        assertWriter(writerContainerName);
        campaigns.put(campaign.campaignId, campaign);
    }

    public List<Campaign> activeCampaigns(String programId) {
        List<Campaign> found = new ArrayList<>();
        for (Campaign c : campaigns.values()) {
            if (c.active && c.programId.equals(programId)) found.add(c);
        }
        return found;
    }
}
