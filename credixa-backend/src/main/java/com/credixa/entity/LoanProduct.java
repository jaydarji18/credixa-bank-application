package com.credixa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal interestRate;

    @Column(name = "min_amount", precision = 18, scale = 2)
    private java.math.BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 2)
    private java.math.BigDecimal maxAmount;

    @Column(name = "min_tenure_months")
    private Integer minTenureMonths;

    @Column(name = "max_tenure_months")
    private Integer maxTenureMonths;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public enum LoanType {
        HOME_LOAN, PERSONAL_LOAN, VEHICLE_LOAN, EDUCATION_LOAN, BUSINESS_LOAN
    }
}
