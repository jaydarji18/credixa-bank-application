package com.credixa.config;

import com.credixa.entity.LoanProduct;
import com.credixa.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig {

    private final LoanProductRepository loanProductRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (loanProductRepository.count() == 0) {
                log.info("Initializing default loan products...");
                
                LoanProduct personalLoan = LoanProduct.builder()
                        .productCode("PL001")
                        .productName("Personal Loan")
                        .loanType(LoanProduct.LoanType.PERSONAL_LOAN)
                        .interestRate(new BigDecimal("10.50"))
                        .minAmount(new BigDecimal("10000"))
                        .maxAmount(new BigDecimal("1500000"))
                        .minTenureMonths(12)
                        .maxTenureMonths(60)
                        .isActive(true)
                        .build();

                LoanProduct homeLoan = LoanProduct.builder()
                        .productCode("HL001")
                        .productName("Home Loan")
                        .loanType(LoanProduct.LoanType.HOME_LOAN)
                        .interestRate(new BigDecimal("8.40"))
                        .minAmount(new BigDecimal("500000"))
                        .maxAmount(new BigDecimal("10000000"))
                        .minTenureMonths(60)
                        .maxTenureMonths(360)
                        .isActive(true)
                        .build();

                LoanProduct vehicleLoan = LoanProduct.builder()
                        .productCode("VL001")
                        .productName("Vehicle Loan")
                        .loanType(LoanProduct.LoanType.VEHICLE_LOAN)
                        .interestRate(new BigDecimal("9.20"))
                        .minAmount(new BigDecimal("100000"))
                        .maxAmount(new BigDecimal("5000000"))
                        .minTenureMonths(12)
                        .maxTenureMonths(84)
                        .isActive(true)
                        .build();

                loanProductRepository.saveAll(List.of(personalLoan, homeLoan, vehicleLoan));
                log.info("Default loan products initialized successfully.");
            }
        };
    }
}
