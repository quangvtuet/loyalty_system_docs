package com.loyalty.redemption_engine.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Component;

/**
 * Client for CT-12: GetMemberTier (Sync/HTTPS REST).
 * Redemption Engine Service → Tiering System Service.
 *
 * SUT collaborator for UC-LB-02 eligibility check (M3).
 */
@Component
public class TieringClient {

    private final RestClient restClient;

    public TieringClient(RestClient.Builder builder,
                         @Value("${loyalty.services.tiering-base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * CT-12 GetMemberTier.
     * In unit/integration tests, this can be mocked or wired.
     */
    public String getMemberTier(String memberId, String programId) {
        try {
            TierResponse response = restClient.get().uri(uriBuilder -> uriBuilder
                            .path("/api/v1/tiering/members/{memberId}/tier")
                            .queryParam("programId", programId).build(memberId))
                    .retrieve().body(TierResponse.class);
            if (response == null || response.currentTier() == null || response.currentTier().isBlank()) {
                throw new IllegalStateException("ERR_RED_TIER_RESPONSE_INVALID: currentTier is missing");
            }
            return response.currentTier();
        } catch (RestClientException ex) {
            throw new IllegalStateException("ERR_RED_TIER_SERVICE_UNAVAILABLE: " + ex.getMessage(), ex);
        }
    }

    public record TierResponse(String memberId, String programId, String currentTier) {}
}
