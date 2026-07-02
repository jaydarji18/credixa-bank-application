package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {
    private Long id;
    private String accountNumber;
    private String accountType;
    private String balance; // Formatted
    private BigDecimal minimumBalance;
    private BigDecimal interestRate;
    private String currency;
    private String status;
    private boolean isPrimary;
    private String branchName;
    private String ifscCode;
    private LocalDateTime createdAt;
}
