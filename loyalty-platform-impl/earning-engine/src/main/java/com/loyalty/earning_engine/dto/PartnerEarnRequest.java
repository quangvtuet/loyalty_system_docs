package com.loyalty.earning_engine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnerEarnRequest {
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "Member ID is required")
    private String memberId;
    
    @NotNull(message = "Spend amount is required")
    @Min(value = 1, message = "Spend amount must be greater than 0")
    private Integer spendAmount;
    
    private String campaignId;
}
