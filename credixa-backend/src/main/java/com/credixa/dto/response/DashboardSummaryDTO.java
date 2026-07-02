package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private BigDecimal totalBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal totalLoanBalance;
    private Integer activeLoanCount;
    private List<ChartDataDTO> spendingTrend;
    @Builder.Default
    private String currency = "INR";
    private String period; // yyyy-MM
}
