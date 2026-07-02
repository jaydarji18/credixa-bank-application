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
public class TransactionResponseDTO {
    private Long id;
    private String referenceNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private String transactionStatus;
    private String description;
    private String category;
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private String beneficiaryName;
    private BigDecimal senderBalance;
}
