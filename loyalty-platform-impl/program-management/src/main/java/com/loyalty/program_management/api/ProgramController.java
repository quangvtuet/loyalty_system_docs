package com.loyalty.program_management.api;

import com.loyalty.program_management.domain.Campaign;
import com.loyalty.program_management.domain.LoyaltyProgram;
import com.loyalty.program_management.service.CampaignService;
import com.loyalty.program_management.service.ProgramService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Program Management REST Controller
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProgramController {

    private final ProgramService programService;
    private final CampaignService campaignService;

    // ─── Programs ───────────────────────────────────────────────

    @PostMapping("/programs")
    public ResponseEntity<LoyaltyProgram> createProgram(@RequestBody LoyaltyProgram request,
                                                        @RequestHeader("X-Operator-Id") UUID operatorId) {
        LoyaltyProgram program = programService.createProgram(request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(program);
    }

    @PostMapping("/programs/{programId}/activate")
    public ResponseEntity<LoyaltyProgram> activateProgram(@PathVariable UUID programId,
                                                          @RequestHeader("X-Operator-Id") UUID operatorId) {
        try {
            LoyaltyProgram program = programService.activateProgram(programId, operatorId);
            return ResponseEntity.ok(program);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/programs")
    public ResponseEntity<List<LoyaltyProgram>> getPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    // ─── Campaigns ───────────────────────────────────────────────

    @PostMapping("/campaigns")
    public ResponseEntity<Campaign> createCampaign(@RequestBody Campaign request,
                                                   @RequestHeader("X-Operator-Id") UUID operatorId) {
        Campaign campaign = campaignService.createCampaign(request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(campaign);
    }

    @GetMapping("/programs/{programId}/campaigns/winning")
    public ResponseEntity<Campaign> getWinningCampaign(@PathVariable UUID programId) {
        Campaign winning = campaignService.getWinningCampaign(programId);
        if (winning == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(winning);
    }
}
