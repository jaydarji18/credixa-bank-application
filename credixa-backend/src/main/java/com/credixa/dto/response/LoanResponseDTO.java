package com.credixa.dto.response;

import com.credixa.entity.Loan;
import com.credixa.entity.LoanProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {
    private Long id;
    private String loanNumber;
    private String productName;
    private LoanProduct.LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal paidAmount;
    private BigDecimal emiAmount;
    private Integer tenureMonths;
    private Loan.LoanStatus status;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private LocalDate nextEmiDate;
    private BigDecimal interestRate;
}
