package com.credixa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "minimum_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    // Fixed Deposit (FD) fields
    @Column(name = "fd_maturity_date")
    private LocalDate fdMaturityDate;

    @Column(name = "fd_maturity_amount", precision = 18, scale = 2)
    private BigDecimal fdMaturityAmount;

    @Column(name = "fd_tenure_months")
    private Integer fdTenureMonths;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AccountType {
        SAVINGS, CURRENT, FIXED_DEPOSIT, RECURRING_DEPOSIT, SALARY, NRI
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, FROZEN, CLOSED, PENDING_ACTIVATION
    }
}
