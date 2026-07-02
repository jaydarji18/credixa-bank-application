package com.credixa.dto.request;

import com.credixa.entity.LoanProduct.LoanType;
import jakarta.validation.constraints.NotBlank;
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
public class LoanProductRequestDTO {
    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Interest rate is required")
    private BigDecimal interestRate;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTenureMonths;
    private Integer maxTenureMonths;
    private boolean isActive;
}
