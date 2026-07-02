package com.credixa.service;

import com.credixa.dto.request.LoanApplicationRequestDTO;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.dto.response.LoanResponseDTO;
import com.credixa.entity.*;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnprocessableEntityException;
import com.credixa.repository.AccountRepository;
import com.credixa.repository.LoanProductRepository;
import com.credixa.repository.LoanRepository;
import com.credixa.repository.UserRepository;
import com.credixa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public List<LoanProductResponseDTO> getLoanProducts() {
        return loanProductRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToProductDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanResponseDTO applyForLoan(String userCode, LoanApplicationRequestDTO request) {
        User user = findUserByCode(userCode);

        // Validate user KYC=VERIFIED
        if (user.getKycStatus() != User.KycStatus.VERIFIED) {
            throw new UnprocessableEntityException("KYC verification required to apply for loans");
        }

        LoanProduct product = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        if (!product.isActive()) {
            throw new BadRequestException("This loan product is currently inactive");
        }

        // Validate amount within product min-max range
        if (request.getRequestedAmount().compareTo(product.getMinAmount()) < 0 ||
                (product.getMaxAmount() != null && request.getRequestedAmount().compareTo(product.getMaxAmount()) > 0)) {
            throw new BadRequestException("Requested amount is outside the allowed range for this product");
        }

        Account linkedAccount = accountRepository.findByIdAndUser(request.getLinkedAccountId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found or access denied"));

        // Calculate EMI: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal monthlyRate = product.getInterestRate()
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        
        int n = request.getTenureMonths();
        BigDecimal p = request.getRequestedAmount();

        BigDecimal onePlusRPowN = monthlyRate.add(BigDecimal.ONE).pow(n, MathContext.DECIMAL128);
        
        BigDecimal emiNumerator = p.multiply(monthlyRate).multiply(onePlusRPowN);
        BigDecimal emiDenominator = onePlusRPowN.subtract(BigDecimal.ONE);
        
        BigDecimal emi = emiNumerator.divide(emiDenominator, 2, RoundingMode.HALF_UP);

        // Generate loanNumber: "LN" + year + padded sequence
        String loanNumber = "LN" + LocalDate.now().getYear() + String.format("%06d", loanRepository.count() + 1);

        Loan loan = Loan.builder()
                .user(user)
                .loanNumber(loanNumber)
                .loanProduct(product)
                .linkedAccount(linkedAccount)
                .principalAmount(p)
                .outstandingBalance(p)
                .emiAmount(emi)
                .tenureMonths(n)
                .status(Loan.LoanStatus.APPLIED)
                .applicationDate(LocalDate.now())
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // Send notification
        notificationService.sendGlobalNotification(user, 
                "Loan Application Received", 
                "Your application for " + product.getProductName() + " of amount ₹" + p + " is under review. Loan Number: " + loanNumber, 
                Notification.NotificationType.LOAN_APPLIED);

        log.info("Loan application submitted for user {}: {}", userCode, loanNumber);
        return mapToLoanDTO(savedLoan);
    }

    @Transactional
    public void disburseLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.APPLIED) {
            throw new BadRequestException("Loan is already " + loan.getStatus());
        }

        Account account = loan.getLinkedAccount();
        BigDecimal amount = loan.getPrincipalAmount();

        // 1. Credit the account
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // 2. Create Transaction record
        Transaction transaction = Transaction.builder()
                .referenceNumber("DISB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .receiverAccount(account)
                .amount(amount)
                .netAmount(amount)
                .transactionType(Transaction.TransactionType.INTEREST_CREDIT) // Using INTEREST_CREDIT as a placeholder for disbursement
                .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                .description("Loan Disbursement: " + loan.getLoanNumber())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // 3. Update Loan Status
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursementDate(LocalDate.now());
        loan.setNextEmiDate(LocalDate.now().plusMonths(1));
        loanRepository.save(loan);

        // 4. Notify User
        notificationService.sendGlobalNotification(loan.getUser(), 
                "Loan Approved & Disbursed!", 
                String.format("Congratulations! ₹%s has been credited to your account %s for loan %s. Your repayment schedule starts from %s.", 
                        amount, account.getAccountNumber(), loan.getLoanNumber(), loan.getNextEmiDate()), 
                Notification.NotificationType.LOAN_APPROVED);
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs at 1 AM every day
    @Transactional
    public void processMonthlyEmis() {
        LocalDate today = LocalDate.now();
        List<Loan> dueLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE && 
                            l.getNextEmiDate() != null && 
                            !l.getNextEmiDate().isAfter(today))
                .collect(Collectors.toList());

        log.info("Processing EMIs for {} loans", dueLoans.size());

        for (Loan loan : dueLoans) {
            try {
                processLoanEmi(loan);
            } catch (Exception e) {
                log.error("Failed to process EMI for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }
    }

    private void processLoanEmi(Loan loan) {
        Account account = loan.getLinkedAccount();
        BigDecimal emi = loan.getEmiAmount();

        if (account.getBalance().compareTo(emi) < 0) {
            // Insufficient balance handling
            notificationService.sendTransactionNotification(loan.getUser().getId(), 
                    "EMI Payment Failed", 
                    "Insufficient balance for EMI of loan " + loan.getLoanNumber(), 
                    Notification.NotificationType.TRANSACTION_FAILED);
            return;
        }

        // Deduct EMI
        account.setBalance(account.getBalance().subtract(emi));
        accountRepository.save(account);

        // Update Outstanding
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(emi));
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus(Loan.LoanStatus.CLOSED);
        } else {
            loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));
        }
        loanRepository.save(loan);

        // Transaction record
        Transaction tx = Transaction.builder()
                .referenceNumber("EMI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .senderAccount(account)
                .amount(emi)
                .netAmount(emi)
                .transactionType(Transaction.TransactionType.EMI_PAYMENT)
                .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                .description("Monthly EMI: " + loan.getLoanNumber())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        notificationService.sendTransactionNotification(loan.getUser().getId(), 
                "EMI Paid", 
                "Monthly EMI of ₹" + emi + " deducted for " + loan.getLoanNumber(), 
                Notification.NotificationType.EMI_REMINDER);
    }

    public List<LoanResponseDTO> getUserLoans(String userCode) {
        User user = findUserByCode(userCode);
        return loanRepository.findByUser(user)
                .stream()
                .map(this::mapToLoanDTO)
                .collect(Collectors.toList());
    }

    public LoanResponseDTO getLoanById(Long loanId, String userCode) {
        User user = findUserByCode(userCode);
        Loan loan = loanRepository.findByIdAndUser(loanId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found or access denied"));
        return mapToLoanDTO(loan);
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LoanProductResponseDTO mapToProductDTO(LoanProduct p) {
        return LoanProductResponseDTO.builder()
                .id(p.getId())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .loanType(p.getLoanType())
                .minAmount(p.getMinAmount())
                .maxAmount(p.getMaxAmount())
                .interestRate(p.getInterestRate())
                .build();
    }

    private LoanResponseDTO mapToLoanDTO(Loan l) {
        return LoanResponseDTO.builder()
                .id(l.getId())
                .loanNumber(l.getLoanNumber())
                .productName(l.getLoanProduct().getProductName())
                .loanType(l.getLoanProduct().getLoanType())
                .principalAmount(l.getPrincipalAmount())
                .outstandingBalance(l.getOutstandingBalance())
                .paidAmount(l.getPrincipalAmount().subtract(l.getOutstandingBalance()))
                .emiAmount(l.getEmiAmount())
                .tenureMonths(l.getTenureMonths())
                .status(l.getStatus())
                .applicationDate(l.getApplicationDate())
                .disbursementDate(l.getDisbursementDate())
                .nextEmiDate(l.getNextEmiDate())
                .interestRate(l.getLoanProduct().getInterestRate())
                .build();
    }
}
