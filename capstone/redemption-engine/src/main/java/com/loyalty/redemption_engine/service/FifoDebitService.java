package com.loyalty.redemption_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Service;

/**
 * FIFO Debit Client/Collaborator — thực thi việc ủy quyền (delegation)
 * thao tác trừ điểm và hoàn điểm cho Earning Engine Service qua contract CT-13.
 *
 * I-7 Source of Truth: Earning DB (thuộc quyền sở hữu duy nhất của Earning Engine Service).
 * Redemption Engine Service KHÔNG BAO GIỜ đọc ghi trực tiếp bảng point_transaction / point_balance.
 *
 * CT-13: DebitPointsFifo & RestorePoints.
 * M5: FIFO Debit Request
 * M8: Reversal Handler
 */
@Service
public class FifoDebitService {

    private final RestClient restClient;

    public FifoDebitService(RestClient.Builder builder,
                            @Value("${loyalty.services.earning-base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * M5 FIFO Debit Request → CT-13: DebitPointsFifo.
     * Ủy quyền cho Earning Engine Service kiểm tra số dư và trừ điểm FIFO.
     */
    public String debitFifo(String memberId, String programId, long pointsRequired, String orderId) {
        EarningDebitResponse response = restClient.post()
                .uri("/api/v1/earning/fifo/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DebitRequest(memberId, programId, pointsRequired, orderId))
                .retrieve()
                .body(EarningDebitResponse.class);
        if (response == null || !"RESERVED".equals(response.status())) {
            throw new IllegalStateException("ERR_RED_EARNING_UPSTREAM: debit reservation was not confirmed");
        }
        return response.allocationId() == null || response.allocationId().isBlank()
                ? orderId : response.allocationId();
    }

    /**
     * M8 Reversal Handler → CT-13: RestorePoints.
     * Ủy quyền cho Earning Engine Service hoàn trả các lô điểm đã trừ về vị trí FIFO gốc (giữ nguyên earn_date và expiry_date).
     * EXC-05 / CON.3.
     */
    public void reverseDebit(String memberId, String programId, String orderId, long pointsToRestore) {
        reverseDebit(memberId, programId, orderId, orderId, pointsToRestore, "FULFILLMENT_FAILED");
    }

    public void reverseDebit(String memberId, String programId, String orderId, String allocationId,
                             long pointsToRestore, String reason) {
        post("/api/v1/earning/fifo/restore", new RestoreRequest(memberId, programId, orderId,
                allocationId, pointsToRestore, reason));
    }

    private void post(String path, Object request) {
        try {
            restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            String detail = ex.getResponseBodyAsString();
            throw new IllegalStateException(detail == null || detail.isBlank()
                    ? "ERR_RED_EARNING_UPSTREAM: HTTP " + ex.getStatusCode().value() : detail, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("ERR_RED_EARNING_SERVICE_UNAVAILABLE: " + ex.getMessage(), ex);
        }
    }

    public record DebitRequest(String memberId, String programId, long pointsRequired, String orderId) {}
    public record RestoreRequest(String memberId, String programId, String orderId, String allocationId,
                                  long pointsToRestore, String reason) {}
    private record EarningDebitResponse(String orderId, String memberId, long pointsDebited, String status,
                                         int debitedBatchesCount, String allocationId, String message) {}
}
