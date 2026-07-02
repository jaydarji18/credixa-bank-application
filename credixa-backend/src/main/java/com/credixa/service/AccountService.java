package com.credixa.service;

import com.credixa.dto.request.CreateAccountRequestDTO;
import com.credixa.dto.response.AccountResponseDTO;
import com.credixa.dto.response.ChartDataDTO;
import com.credixa.dto.response.DashboardSummaryDTO;
import java.time.format.DateTimeFormatter;
import com.credixa.entity.Account;
import com.credixa.entity.Branch;
import com.credixa.entity.Transaction;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    public List<AccountResponseDTO> getUserAccounts(String userCode) {
        User user = findUserByCode(userCode);
        return accountRepository.findByUserIdOrderByIsPrimaryDesc(user.getId()).stream()
                .filter(acc -> acc.getStatus() != Account.AccountStatus.CLOSED)
                .map(this::mapToAccountDTO)
                .collect(Collectors.toList());
    }

    public AccountResponseDTO getAccountById(Long accountId, String userCode) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getUserCode().equals(userCode)) {
            throw new BadRequestException("You do not have permission to access this account");
        }

        return mapToAccountDTO(account);
    }

    public DashboardSummaryDTO getDashboardSummary(String userCode) {
        User user = findUserByCode(userCode);
        Long userId = user.getId();

        // Total Balance
        BigDecimal totalBalance = accountRepository.findByUserIdOrderByIsPrimaryDesc(userId).stream()
                .filter(acc -> acc.getStatus() == Account.AccountStatus.ACTIVE)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Monthly Income/Expenses
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        
        List<Transaction.TransactionType> incomeTypes = List.of(Transaction.TransactionType.DEPOSIT, Transaction.TransactionType.INTEREST_CREDIT);
        BigDecimal monthlyIncome = transactionRepository.sumIncomeForMonth(userId, incomeTypes, startOfMonth);
        if (monthlyIncome == null) monthlyIncome = BigDecimal.ZERO;

        List<Transaction.TransactionType> expenseTypes = List.of(
            Transaction.TransactionType.WITHDRAWAL, 
            Transaction.TransactionType.TRANSFER_INTERNAL,
            Transaction.TransactionType.TRANSFER_NEFT,
            Transaction.TransactionType.TRANSFER_RTGS,
            Transaction.TransactionType.TRANSFER_IMPS,
            Transaction.TransactionType.TRANSFER_UPI,
            Transaction.TransactionType.BILL_PAYMENT,
            Transaction.TransactionType.ATM_WITHDRAWAL,
            Transaction.TransactionType.EMI_PAYMENT
        );
        BigDecimal monthlyExpenses = transactionRepository.sumExpensesForMonth(userId, expenseTypes, startOfMonth);
        if (monthlyExpenses == null) monthlyExpenses = BigDecimal.ZERO;

        // Total Loan Balance & Count
        List<com.credixa.entity.Loan> activeLoans = loanRepository.findByUserAndStatus(user, com.credixa.entity.Loan.LoanStatus.ACTIVE);
        BigDecimal totalLoanBalance = activeLoans.stream()
                .map(com.credixa.entity.Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int activeLoanCount = activeLoans.size();

        // Spending Trend (Last 30 days)
        List<Object[]> rawTrend = transactionRepository.sumDailyExpenses(userId, expenseTypes, LocalDateTime.now().minusDays(30));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        List<ChartDataDTO> spendingTrend = rawTrend.stream()
                .map(obj -> {
                    LocalDate date = ((java.sql.Date) obj[0]).toLocalDate();
                    return ChartDataDTO.builder()
                        .name(date.format(formatter))
                        .value((BigDecimal) obj[1])
                        .build();
                })
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalBalance(totalBalance)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .totalLoanBalance(totalLoanBalance)
                .activeLoanCount(activeLoanCount)
                .spendingTrend(spendingTrend)
                .period(YearMonth.now().toString())
                .build();
    }

    @Transactional
    public AccountResponseDTO createAccount(String userCode, CreateAccountRequestDTO request) {
        User user = findUserByCode(userCode);

        if (user.getStatus() != User.UserStatus.ACTIVE || user.getKycStatus() != User.KycStatus.VERIFIED) {
            throw new BadRequestException("User must be ACTIVE and KYC VERIFIED to create an account");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        // Generate Account Number: YYYYMMDD + 4 random digits
        String accountNumber = LocalDate.now().toString().replace("-", "") + String.format("%04d", new Random().nextInt(10000));

        BigDecimal minBalance;
        BigDecimal interestRate;

        switch (request.getAccountType()) {
            case SAVINGS -> {
                minBalance = new BigDecimal("500");
                interestRate = new BigDecimal("3.5");
            }
            case CURRENT -> {
                minBalance = new BigDecimal("5000");
                interestRate = BigDecimal.ZERO;
            }
            case SALARY -> {
                minBalance = BigDecimal.ZERO;
                interestRate = new BigDecimal("3.5");
            }
            case FIXED_DEPOSIT -> {
                if (request.getFdTenureMonths() == null) throw new BadRequestException("Tenure required for FD");
                minBalance = BigDecimal.ZERO;
                interestRate = new BigDecimal("7.0");
            }
            default -> {
                minBalance = new BigDecimal("500");
                interestRate = new BigDecimal("3.5");
            }
        }

        BigDecimal initialDeposit = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;
        
        // Ensure initial balance is at least the minBalance if it's a new account requirement
        BigDecimal balance = initialDeposit.compareTo(minBalance) >= 0 ? initialDeposit : minBalance;

        Account account = Account.builder()
                .user(user)
                .branch(branch)
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .status(Account.AccountStatus.ACTIVE) // Demo mode: ACTIVE immediately
                .balance(balance)
                .minimumBalance(minBalance)
                .interestRate(interestRate)
                .currency("INR")
                .isPrimary(accountRepository.findByUserIdOrderByIsPrimaryDesc(user.getId()).isEmpty())
                .fdTenureMonths(request.getFdTenureMonths())
                .build();

        Account savedAccount = accountRepository.save(account);

        // Create initial deposit transaction if amount > 0
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction deposit = Transaction.builder()
                    .senderAccount(null)
                    .receiverAccount(savedAccount)
                    .amount(balance)
                    .netAmount(balance) // Fixed: net_amount cannot be null
                    .fee(BigDecimal.ZERO)
                    .transactionType(Transaction.TransactionType.DEPOSIT)
                    .transactionStatus(Transaction.TransactionStatus.SUCCESS) // Use SUCCESS instead of COMPLETED for consistency
                    .referenceNumber("INIT-" + System.currentTimeMillis())
                    .description("Initial deposit for account opening")
                    .initiatedAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(deposit);
        }

        return mapToAccountDTO(savedAccount);
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AccountResponseDTO mapToAccountDTO(Account account) {
        return AccountResponseDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .balance(String.format("%.2f", account.getBalance()))
                .minimumBalance(account.getMinimumBalance())
                .interestRate(account.getInterestRate())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .isPrimary(account.isPrimary())
                .branchName(account.getBranch().getBranchName())
                .ifscCode(account.getBranch().getIfscCode())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
