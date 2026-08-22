package com.loyalty.tiering_system.api;

import com.loyalty.tiering_system.domain.MemberTier;
import com.loyalty.tiering_system.repository.MemberTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Tiering REST Controller — implements CT-12 GetMemberTier.
 * OpenAPI operationId: getMemberTier
 * Path: GET /api/v1/tiering/members/{memberId}/tier
 */
@RestController
@RequestMapping("/api/v1/tiering")
@RequiredArgsConstructor
public class TierController {

    private final MemberTierRepository memberTierRepository;

    @GetMapping("/members/{memberId}/tier")
    public ResponseEntity<?> getMemberTier(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "DEFAULT_PROG") String programId) {

        return memberTierRepository.findByMemberIdAndProgramId(memberId, programId)
                .map(tier -> ResponseEntity.ok((Object) tier))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "type", "urn:loyalty:error:tier-not-found",
                        "title", "Tier record not found",
                        "status", 404,
                        "detail", "No tier record found for member " + memberId + " in program " + programId
                )));
    }
}
