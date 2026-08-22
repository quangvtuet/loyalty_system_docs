package com.loyalty.earning_engine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FifoRestoreRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;

    private String programId;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    private String allocationId;

    @NotNull(message = "Points to restore is mandatory")
    @Min(value = 1, message = "Points to restore must be >= 1")
    private Long pointsToRestore;

    private String reason;
}
