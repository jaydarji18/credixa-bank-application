package com.credixa.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequestDTO {

    @NotNull(message = "Loan product ID is required")
    private Long loanProductId;

    @NotNull(message = "Linked account ID is required")
    private Long linkedAccountId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1000", message = "Minimum loan amount is 1000")
    private BigDecimal requestedAmount;

    @NotNull(message = "Tenure is required")
    private Integer tenureMonths;
}
