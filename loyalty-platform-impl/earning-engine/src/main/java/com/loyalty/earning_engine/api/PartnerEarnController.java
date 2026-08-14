package com.loyalty.earning_engine.api;

import com.loyalty.earning_engine.dto.PartnerEarnRequest;
import com.loyalty.earning_engine.service.EarnCalculator;
import com.loyalty.earning_engine.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/partners/earn")
@RequiredArgsConstructor
public class PartnerEarnController {

    private final EarnCalculator earnCalculator;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<?> submitEarn(@Valid @RequestBody PartnerEarnRequest request) {
        
        boolean isDuplicate = idempotencyService.isDuplicate(request.getTransactionId(), "PARTNER");
        if (isDuplicate) {
            return ResponseEntity.status(409).body(Map.of(
                    "status", 409,
                    "message", "Duplicate transaction detected"
            ));
        }

        earnCalculator.processEarn(
                request.getMemberId(),
                request.getSpendAmount(),
                request.getTransactionId(),
                "SILVER", // Default tier for partner earn in this mock
                request.getCampaignId()
        );

        return ResponseEntity.accepted().body(Map.of(
                "status", 202,
                "message", "Earn request accepted and processed successfully"
        ));
    }
}
