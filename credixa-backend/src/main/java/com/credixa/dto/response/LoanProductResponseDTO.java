package com.credixa.dto.response;

import com.credixa.entity.LoanProduct.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductResponseDTO {
    private Long id;
    private String productCode;
    private String productName;
    private LoanType loanType;
    private BigDecimal interestRate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTenureMonths;
    private Integer maxTenureMonths;
    private boolean isActive;
}
