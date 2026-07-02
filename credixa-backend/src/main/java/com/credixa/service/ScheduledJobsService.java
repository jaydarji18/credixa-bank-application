package com.credixa.service;

import com.credixa.entity.Account;
import com.credixa.entity.Loan;
import com.credixa.entity.Notification;
import com.credixa.entity.Transaction;
import com.credixa.repository.AccountRepository;
import com.credixa.repository.LoanRepository;
import com.credixa.repository.TransactionRepository;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    /**
     * Daily 1 AM: Monthly Statement Email Job
     * Finds users active in last 30 days and logs statement summary.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void monthlyStatementJob() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long userCount = userRepository.countByCreatedAtAfter(thirtyDaysAgo); // Placeholder for active users
        
        String period = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Sending monthly statement email to {} users for period {}", userCount, period);
    }

    /**
     * Daily 9 AM: EMI Reminder Job
     * Reminds users of upcoming EMIs due in 3 days.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void emiReminderJob() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        List<Loan> upcomingLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE && 
                           l.getNextEmiDate() != null && 
                           l.getNextEmiDate().equals(targetDate))
                .toList();

        for (Loan loan : upcomingLoans) {
            notificationService.sendTransactionNotification(
                    loan.getUser().getId(),
                    "EMI Reminder",
                    String.format("Reminder: Your EMI of ₹%s for loan %s is due on %s", 
                            loan.getEmiAmount(), loan.getLoanNumber(), targetDate),
                    Notification.NotificationType.EMI_REMINDER
            );
        }

        log.info("EMI Reminder Job: Processed {} reminders", upcomingLoans.size());
    }

    /**
     * 1st of each month 2 AM: Interest Credit Job
     * Credits monthly interest to SAVINGS and SALARY accounts.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void interestCreditJob() {
        List<Account> targetAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getStatus() == Account.AccountStatus.ACTIVE && 
                           (a.getAccountType() == Account.AccountType.SAVINGS || 
                            a.getAccountType() == Account.AccountType.SALARY))
                .toList();

        int processedCount = 0;
        for (Account account : targetAccounts) {
            if (account.getInterestRate() != null && account.getInterestRate().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal monthlyInterest = account.getBalance()
                        .multiply(account.getInterestRate())
                        .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);

                if (monthlyInterest.compareTo(BigDecimal.ZERO) > 0) {
                    account.setBalance(account.getBalance().add(monthlyInterest));
                    accountRepository.save(account);

                    Transaction transaction = Transaction.builder()
                            .referenceNumber("INT" + System.currentTimeMillis() + new Random().nextInt(1000))
                            .receiverAccount(account)
                            .amount(monthlyInterest)
                            .netAmount(monthlyInterest)
                            .transactionType(Transaction.TransactionType.INTEREST_CREDIT)
                            .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                            .description("Monthly interest credit")
                            .processedAt(LocalDateTime.now())
                            .build();

                    transactionRepository.save(transaction);
                    processedCount++;
                }
            }
        }

        log.info("Interest Credit Job: Credited interest to {} accounts", processedCount);
    }
}
