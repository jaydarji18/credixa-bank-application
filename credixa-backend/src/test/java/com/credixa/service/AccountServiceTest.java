package com.credixa.service;

import com.credixa.dto.request.CreateAccountRequestDTO;
import com.credixa.dto.response.AccountResponseDTO;
import com.credixa.dto.response.DashboardSummaryDTO;
import com.credixa.entity.Account;
import com.credixa.entity.Branch;
import com.credixa.entity.Transaction;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Branch testBranch;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(User.UserStatus.ACTIVE)
                .kycStatus(User.KycStatus.VERIFIED)
                .build();

        testBranch = Branch.builder()
                .id(1L)
                .branchName("Main Branch")
                .ifscCode("CRDX0001")
                .city("Mumbai")
                .build();
    }

    @Nested
    @DisplayName("getUserAccounts() tests")
    class GetUserAccountsTests {

        @Test
        @DisplayName("Should return user accounts successfully")
        void shouldReturnUserAccounts() {
Account account1 = Account.builder()
                     .id(1L)
                     .accountNumber("1234567890")
                     .accountType(Account.AccountType.SAVINGS)
                     .status(Account.AccountStatus.ACTIVE)
                     .user(testUser)
                     .branch(testBranch)
                     .build();

             Account account2 = Account.builder()
                     .id(2L)
                     .accountNumber("0987654321")
                     .accountType(Account.AccountType.CURRENT)
                     .status(Account.AccountStatus.CLOSED)
                     .user(testUser)
                     .branch(testBranch)
                     .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L))
                    .willReturn(List.of(account1, account2));

            List<AccountResponseDTO> response = accountService.getUserAccounts("USR00001");

            assertThat(response).hasSize(1); // Only ACTIVE accounts returned
            assertThat(response.get(0).getAccountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("Should return empty list when user has no accounts")
        void shouldReturnEmptyListWhenNoAccounts() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());

            List<AccountResponseDTO> response = accountService.getUserAccounts("USR00001");

            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getUserAccounts("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAccountById() tests")
    class GetAccountByIdTests {

@Test
        @DisplayName("Should return account successfully for owner")
        void shouldReturnAccountForOwner() {
            Account account = Account.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .accountType(Account.AccountType.SAVINGS)
                    .user(testUser)
                    .status(Account.AccountStatus.ACTIVE)
                    .branch(testBranch)
                    .build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            AccountResponseDTO response = accountService.getAccountById(1L, "USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getAccountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not account owner")
        void shouldThrowExceptionWhenUserNotOwner() {
            User otherUser = User.builder().id(2L).userCode("USR00002").build();
            Account account = Account.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .user(otherUser)
                    .build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            assertThatThrownBy(() -> accountService.getAccountById(1L, "USR00001"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("do not have permission");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            given(accountRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountById(999L, "USR00001"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDashboardSummary() tests")
    class DashboardSummaryTests {

        @Test
        @DisplayName("Should return dashboard summary successfully")
        void shouldReturnDashboardSummary() {
Account account = Account.builder()
                     .id(1L)
                     .user(testUser)
                     .status(Account.AccountStatus.ACTIVE)
                     .balance(new BigDecimal("50000"))
                     .branch(testBranch)
                     .build();

            java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
            Object[] trendData = new Object[]{today, new BigDecimal("1000")};

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of(account));
            given(transactionRepository.sumIncomeForMonth(anyLong(), anyList(), any()))
                    .willReturn(new BigDecimal("100000"));
            given(transactionRepository.sumExpensesForMonth(anyLong(), anyList(), any()))
                    .willReturn(new BigDecimal("50000"));
            given(loanRepository.findByUserAndStatus(eq(testUser), any()))
                    .willReturn(List.of());
            given(transactionRepository.sumDailyExpenses(anyLong(), anyList(), any()))
                    .willReturn(java.util.Collections.singletonList(trendData));

            DashboardSummaryDTO response = accountService.getDashboardSummary("USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getTotalBalance()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(response.getMonthlyIncome()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(response.getMonthlyExpenses()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("Should handle null transaction sums gracefully")
        void shouldHandleNullTransactionSums() {
Account account = Account.builder()
                     .id(1L)
                     .user(testUser)
                     .status(Account.AccountStatus.ACTIVE)
                     .balance(new BigDecimal("50000"))
                     .branch(testBranch)
                     .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of(account));
            given(transactionRepository.sumIncomeForMonth(anyLong(), anyList(), any()))
                    .willReturn(null);
            given(transactionRepository.sumExpensesForMonth(anyLong(), anyList(), any()))
                    .willReturn(null);

            DashboardSummaryDTO response = accountService.getDashboardSummary("USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getMonthlyIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getMonthlyExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for dashboard when user not found")
        void shouldThrowExceptionWhenUserNotFoundForDashboard() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getDashboardSummary("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createAccount() tests")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create savings account successfully")
        void shouldCreateSavingsAccount() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SAVINGS)
                    .branchId(1L)
                    .initialDeposit(new BigDecimal("1000"))
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(1L);
                return acc;
            });
            given(transactionRepository.save(any())).willReturn(Transaction.builder().build());

            AccountResponseDTO response = accountService.createAccount("USR00001", request);

            assertThat(response).isNotNull();
            verify(accountRepository).save(argThat(acc -> 
                    acc.getMinimumBalance().compareTo(new BigDecimal("500")) == 0 &&
                    acc.getInterestRate().compareTo(new BigDecimal("3.5")) == 0
            ));
        }

        @Test
        @DisplayName("Should create current account with higher minimum balance")
        void shouldCreateCurrentAccount() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.CURRENT)
                    .branchId(1L)
                    .initialDeposit(new BigDecimal("50000"))
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(1L);
                return acc;
            });

            accountService.createAccount("USR00001", request);

            verify(accountRepository).save(argThat(acc -> 
                    acc.getMinimumBalance().compareTo(new BigDecimal("5000")) == 0 &&
                    acc.getInterestRate().compareTo(BigDecimal.ZERO) == 0
            ));
        }

        @Test
        @DisplayName("Should create salary account with zero minimum balance")
        void shouldCreateSalaryAccount() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SALARY)
                    .branchId(1L)
                    .initialDeposit(new BigDecimal("1000"))
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(1L);
                return acc;
            });

            accountService.createAccount("USR00001", request);

            verify(accountRepository).save(argThat(acc -> 
                    acc.getMinimumBalance().compareTo(BigDecimal.ZERO) == 0
            ));
        }

        @Test
        @DisplayName("Should create fixed deposit account with tenure requirement")
        void shouldCreateFdAccount() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.FIXED_DEPOSIT)
                    .branchId(1L)
                    .initialDeposit(new BigDecimal("100000"))
                    .fdTenureMonths(12)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(1L);
                return acc;
            });

            accountService.createAccount("USR00001", request);

            verify(accountRepository).save(argThat(acc -> 
                    acc.getInterestRate().compareTo(new BigDecimal("7.0")) == 0 &&
                    acc.getFdTenureMonths().equals(12)
            ));
        }

        @Test
        @DisplayName("Should throw BadRequestException when FD tenure missing")
        void shouldThrowExceptionWhenFdTenureMissing() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.FIXED_DEPOSIT)
                    .branchId(1L)
                    .initialDeposit(new BigDecimal("100000"))
                    .fdTenureMonths(null)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));

            assertThatThrownBy(() -> accountService.createAccount("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tenure required for FD");
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not ACTIVE and KYC VERIFIED")
        void shouldThrowExceptionWhenUserNotActiveOrKycVerified() {
            testUser.setStatus(User.UserStatus.PENDING_VERIFICATION);
            testUser.setKycStatus(User.KycStatus.PENDING);

            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SAVINGS)
                    .branchId(1L)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> accountService.createAccount("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must be ACTIVE and KYC VERIFIED");
        }

        @Test
        @DisplayName("Should set account as primary when user has no accounts")
        void shouldSetAccountAsPrimaryWhenFirst() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SAVINGS)
                    .branchId(1L)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L)).willReturn(List.of());
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(1L);
                return acc;
            });

            accountService.createAccount("USR00001", request);

            verify(accountRepository).save(argThat(acc -> acc.isPrimary() == true));
        }

        @Test
        @DisplayName("Should not set account as primary when user has existing accounts")
        void shouldNotSetAccountAsPrimaryWhenNotFirst() {
            Account existingAccount = Account.builder()
                    .id(1L)
                    .isPrimary(true)
                    .user(testUser)
                    .build();

            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SAVINGS)
                    .branchId(1L)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(1L)).willReturn(Optional.of(testBranch));
            given(accountRepository.findByUserIdOrderByIsPrimaryDesc(1L))
                    .willReturn(List.of(existingAccount));
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setId(2L);
                return acc;
            });

            accountService.createAccount("USR00001", request);

            verify(accountRepository).save(argThat(acc -> acc.isPrimary() == false));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when branch not found")
        void shouldThrowExceptionWhenBranchNotFound() {
            CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                    .accountType(Account.AccountType.SAVINGS)
                    .branchId(999L)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(branchRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.createAccount("USR00001", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Branch not found");
        }
    }
}