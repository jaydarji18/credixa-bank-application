package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdateDTO {
    private Long accountId;
    private BigDecimal newBalance;
    private LastTransaction lastTransaction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastTransaction {
        private String ref;
        private BigDecimal amount;
        private String type;
    }
}
