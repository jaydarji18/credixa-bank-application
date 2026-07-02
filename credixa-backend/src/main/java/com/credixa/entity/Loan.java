package com.credixa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", nullable = false, unique = true)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id", nullable = false)
    private Account linkedAccount;

    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "emi_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LoanStatus {
        APPLIED, UNDER_REVIEW, SANCTIONED, DISBURSED, ACTIVE, CLOSED
    }
}
