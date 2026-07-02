package com.credixa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id")
    private Account receiverAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private Beneficiary beneficiary;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    private TransactionStatus transactionStatus;

    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    private String description;

    @CreationTimestamp
    @Column(name = "initiated_at", updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_INTERNAL, TRANSFER_NEFT, TRANSFER_RTGS, TRANSFER_IMPS, TRANSFER_UPI, BILL_PAYMENT, ATM_WITHDRAWAL, INTEREST_CREDIT, EMI_PAYMENT
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED, REVERSED, COMPLETED
    }

    public enum TransactionCategory {
        FOOD, SHOPPING, UTILITIES, ENTERTAINMENT, TRANSPORT, OTHER
    }
}
