package com.credixa.dto.request;

import com.credixa.entity.Account;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequestDTO {
    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Integer fdTenureMonths; // Required if accountType is FIXED_DEPOSIT

    private java.math.BigDecimal initialDeposit;
}
