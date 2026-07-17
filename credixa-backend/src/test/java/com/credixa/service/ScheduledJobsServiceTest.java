package com.credixa.service;

import com.credixa.entity.Account;
import com.credixa.entity.Loan;
import com.credixa.entity.Transaction;
import com.credixa.entity.User;
import com.credixa.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ScheduledJobsService scheduledJobsService;

    @Nested
    @DisplayName("monthlyStatementJob() tests")
    class MonthlyStatementJobTests {

        @Test
        @DisplayName("Should process monthly statement job without errors")
        void shouldProcessMonthlyStatementJob() {
            given(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(100L);

            assertThatCode(() -> scheduledJobsService.monthlyStatementJob())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("emiReminderJob() tests")
    class EmiReminderJobTests {

        @Test
        @DisplayName("Should process EMI reminders for upcoming loans")
        void shouldProcessEmiReminders() {
            User user = User.builder()
                    .id(1L)
                    .firstName("John")
                    .build();

            Loan loan = Loan.builder()
                    .id(1L)
                    .loanNumber("LN2024001")
                    .user(user)
                    .emiAmount(new BigDecimal("5000"))
                    .nextEmiDate(LocalDate.now().plusDays(3))
                    .status(Loan.LoanStatus.ACTIVE)
                    .build();

            given(loanRepository.findAll()).willReturn(List.of(loan));

            assertThatCode(() -> scheduledJobsService.emiReminderJob())
                    .doesNotThrowAnyException();

            verify(notificationService).sendTransactionNotification(
                    anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should not send reminder for inactive loans")
        void shouldNotSendReminderForInactiveLoans() {
            User user = User.builder().id(1L).build();
            Loan loan = Loan.builder()
                    .id(1L)
                    .status(Loan.LoanStatus.CLOSED)
                    .nextEmiDate(LocalDate.now().plusDays(3))
                    .build();

            given(loanRepository.findAll()).willReturn(List.of(loan));

            scheduledJobsService.emiReminderJob();

            verify(notificationService, never()).sendTransactionNotification(
                    anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should not send reminder for loans with past EMI date")
        void shouldNotSendReminderForPastEmiDate() {
            User user = User.builder().id(1L).build();
            Loan loan = Loan.builder()
                    .id(1L)
                    .status(Loan.LoanStatus.ACTIVE)
                    .nextEmiDate(LocalDate.now().minusDays(1))
                    .build();

            given(loanRepository.findAll()).willReturn(List.of(loan));

            scheduledJobsService.emiReminderJob();

            verify(notificationService, never()).sendTransactionNotification(
                    anyLong(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("interestCreditJob() tests")
    class InterestCreditJobTests {

        @Test
        @DisplayName("Should credit interest to savings accounts")
        void shouldCreditInterestToSavingsAccounts() {
            Account account = Account.builder()
                    .id(1L)
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("3.5"))
                    .build();

            given(accountRepository.findAll()).willReturn(List.of(account));
            given(accountRepository.save(any(Account.class))).willAnswer(i -> i.getArgument(0));
            given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));

            assertThatCode(() -> scheduledJobsService.interestCreditJob())
                    .doesNotThrowAnyException();

            verify(accountRepository).save(argThat(acc -> 
                    acc.getBalance().compareTo(new BigDecimal("100000")) > 0
            ));
        }

        @Test
        @DisplayName("Should credit interest to salary accounts")
        void shouldCreditInterestToSalaryAccounts() {
            Account account = Account.builder()
                    .id(1L)
                    .accountType(Account.AccountType.SALARY)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("50000"))
                    .interestRate(new BigDecimal("3.5"))
                    .build();

            given(accountRepository.findAll()).willReturn(List.of(account));
            given(accountRepository.save(any(Account.class))).willAnswer(i -> i.getArgument(0));
            given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));

            scheduledJobsService.interestCreditJob();

            verify(transactionRepository).save(argThat(tx -> 
                    tx.getTransactionType() == Transaction.TransactionType.INTEREST_CREDIT
            ));
        }

        @Test
        @DisplayName("Should not credit interest to frozen accounts")
        void shouldNotCreditInterestToFrozenAccounts() {
            Account account = Account.builder()
                    .id(1L)
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.FROZEN)
                    .build();

            given(accountRepository.findAll()).willReturn(List.of(account));

            scheduledJobsService.interestCreditJob();

            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not credit interest when interest rate is zero")
        void shouldNotCreditInterestWhenRateZero() {
            Account account = Account.builder()
                    .id(1L)
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .interestRate(BigDecimal.ZERO)
                    .build();

            given(accountRepository.findAll()).willReturn(List.of(account));

            scheduledJobsService.interestCreditJob();

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should process multiple accounts")
        void shouldProcessMultipleAccounts() {
            Account account1 = Account.builder()
                    .id(1L)
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .interestRate(new BigDecimal("3.5"))
                    .balance(new BigDecimal("1000"))
                    .build();

            Account account2 = Account.builder()
                    .id(2L)
                    .accountType(Account.AccountType.SALARY)
                    .status(Account.AccountStatus.ACTIVE)
                    .interestRate(new BigDecimal("4.0"))
                    .balance(new BigDecimal("1000"))
                    .build();

            given(accountRepository.findAll()).willReturn(List.of(account1, account2));
            given(accountRepository.save(any(Account.class))).willAnswer(i -> i.getArgument(0));
            given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));

            scheduledJobsService.interestCreditJob();

            verify(transactionRepository, times(2)).save(any());
        }
    }
}