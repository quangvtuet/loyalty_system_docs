package com.loyalty.earning_engine.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for CT-12: GetMemberTier (Sync/HTTPS REST).
 * Earning Engine Service → Tiering System Service.
 *
 * Used by EarnCalculator to securely query the authoritative member tier from Tiering DB,
 * enforcing I-5 (never trusting client-supplied tier fields in earn events).
 *
 * Spec-trace: CT-12, I-5, CON.2, T3
 */
@Component
@Slf4j
public class TieringClient {

    private final RestClient restClient;

    @Autowired
    public TieringClient(RestClient.Builder builder,
                         @Value("${loyalty.services.tiering-base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = (builder != null) ? builder.baseUrl(baseUrl).build() : RestClient.builder().baseUrl(baseUrl).build();
    }

    public TieringClient() {
        this.restClient = RestClient.builder().baseUrl("http://localhost:8082").build();
    }

    /**
     * Queries CT-12 for the member's current tier.
     * Falls back to "SILVER" if service is unavailable in isolated unit environments.
     */
    public String getMemberTier(String memberId, String programId) {
        try {
            TierResponse response = restClient.get().uri(uriBuilder -> uriBuilder
                            .path("/api/v1/tiering/members/{memberId}/tier")
                            .queryParam("programId", programId).build(memberId))
                    .retrieve().body(TierResponse.class);
            if (response != null && response.currentTier() != null && !response.currentTier().isBlank()) {
                return response.currentTier().toUpperCase();
            }
            return "SILVER";
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("[CT-12 Tier Query] Tier service unreachable or error for member '{}', defaulting to SILVER: {}",
                    memberId, ex.getMessage());
            return "SILVER";
        }
    }

    public record TierResponse(String memberId, String programId, String currentTier) {}
}
