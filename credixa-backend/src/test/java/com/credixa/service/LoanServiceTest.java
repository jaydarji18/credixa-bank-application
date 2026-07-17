package com.credixa.service;

import com.credixa.dto.request.LoanApplicationRequestDTO;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.dto.response.LoanResponseDTO;
import com.credixa.entity.*;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnprocessableEntityException;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanService loanService;

    private User testUser;
    private LoanProduct testLoanProduct;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .lastName("Doe")
                .kycStatus(User.KycStatus.VERIFIED)
                .status(User.UserStatus.ACTIVE)
                .build();

        testLoanProduct = LoanProduct.builder()
                .id(1L)
                .productCode("LP001")
                .productName("Personal Loan")
                .loanType(LoanProduct.LoanType.PERSONAL_LOAN)
                .interestRate(new BigDecimal("12.5"))
                .minAmount(new BigDecimal("50000"))
                .maxAmount(new BigDecimal("500000"))
                .isActive(true)
                .build();

        testAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .user(testUser)
                .status(Account.AccountStatus.ACTIVE)
                .balance(new BigDecimal("50000"))
                .build();
    }

    @Nested
    @DisplayName("getLoanProducts() tests")
    class GetLoanProductsTests {

        @Test
        @DisplayName("Should return active loan products only")
        void shouldReturnActiveLoanProducts() {
            LoanProduct activeProduct = LoanProduct.builder()
                    .id(1L)
                    .isActive(true)
                    .build();
            LoanProduct inactiveProduct = LoanProduct.builder()
                    .id(2L)
                    .isActive(false)
                    .build();

            given(loanProductRepository.findByIsActiveTrue()).willReturn(List.of(activeProduct));

            List<LoanProductResponseDTO> response = loanService.getLoanProducts();

            assertThat(response).hasSize(1);
            verify(loanProductRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should return empty list when no active products")
        void shouldReturnEmptyListWhenNoActiveProducts() {
            given(loanProductRepository.findByIsActiveTrue()).willReturn(List.of());

            List<LoanProductResponseDTO> response = loanService.getLoanProducts();

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("applyForLoan() tests")
    class ApplyForLoanTests {

        @Test
        @DisplayName("Should apply for loan successfully")
        void shouldApplyForLoanSuccessfully() {
            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("100000"))
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(1L)).willReturn(Optional.of(testLoanProduct));
            given(accountRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(testAccount));
            given(loanRepository.count()).willReturn(5L);
            given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> {
                Loan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            LoanResponseDTO response = loanService.applyForLoan("USR00001", request);

            assertThat(response).isNotNull();
            assertThat(response.getLoanNumber()).startsWith("LN");
            verify(notificationService).sendGlobalNotification(any(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw UnprocessableEntityException when KYC not verified")
        void shouldThrowExceptionWhenKycNotVerified() {
            testUser.setKycStatus(User.KycStatus.NOT_SUBMITTED);

            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("100000"))
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(UnprocessableEntityException.class)
                    .hasMessageContaining("KYC verification required");
        }

        @Test
        @DisplayName("Should throw BadRequestException when loan product not found")
        void shouldThrowExceptionWhenLoanProductNotFound() {
            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(999L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("100000"))
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Loan product not found");
        }

        @Test
        @DisplayName("Should throw BadRequestException when loan product is inactive")
        void shouldThrowExceptionWhenLoanProductInactive() {
            testLoanProduct.setActive(false);

            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("100000"))
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(1L)).willReturn(Optional.of(testLoanProduct));

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This loan product is currently inactive");
        }

        @Test
        @DisplayName("Should throw BadRequestException when amount below minimum")
        void shouldThrowExceptionWhenAmountBelowMinimum() {
            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("10000")) // Below min of 50000
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(1L)).willReturn(Optional.of(testLoanProduct));

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("outside the allowed range");
        }

        @Test
        @DisplayName("Should throw BadRequestException when amount above maximum")
        void shouldThrowExceptionWhenAmountAboveMaximum() {
            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(1L)
                    .requestedAmount(new BigDecimal("600000")) // Above max of 500000
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(1L)).willReturn(Optional.of(testLoanProduct));

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("outside the allowed range");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when linked account not found")
        void shouldThrowExceptionWhenLinkedAccountNotFound() {
            LoanApplicationRequestDTO request = LoanApplicationRequestDTO.builder()
                    .loanProductId(1L)
                    .linkedAccountId(999L)
                    .requestedAmount(new BigDecimal("100000"))
                    .tenureMonths(24)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanProductRepository.findById(1L)).willReturn(Optional.of(testLoanProduct));
            given(accountRepository.findByIdAndUser(999L, testUser)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.applyForLoan("USR00001", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Linked account not found or access denied");
        }
    }

    @Nested
    @DisplayName("disburseLoan() tests")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should disburse loan successfully")
        void shouldDisburseLoanSuccessfully() {
            Loan loan = Loan.builder()
                    .id(1L)
                    .loanNumber("LN2024001")
                    .user(testUser)
                    .linkedAccount(testAccount)
                    .principalAmount(new BigDecimal("100000"))
                    .status(Loan.LoanStatus.APPLIED)
                    .build();

            given(loanRepository.findById(1L)).willReturn(Optional.of(loan));
            given(accountRepository.save(any(Account.class))).willAnswer(i -> i.getArgument(0));
            given(transactionRepository.save(any())).willReturn(Transaction.builder().build());
            given(loanRepository.save(any(Loan.class))).willAnswer(i -> i.getArgument(0));

            assertThatCode(() -> loanService.disburseLoan(1L)).doesNotThrowAnyException();

            verify(accountRepository).save(argThat(acc -> 
                    acc.getBalance().compareTo(new BigDecimal("150000")) == 0
            ));
            verify(loanRepository).save(argThat(l -> 
                    l.getStatus() == Loan.LoanStatus.ACTIVE &&
                    l.getDisbursementDate() != null &&
                    l.getNextEmiDate() != null
            ));
        }

        @Test
        @DisplayName("Should throw BadRequestException when loan already processed")
        void shouldThrowExceptionWhenLoanAlreadyProcessed() {
            Loan loan = Loan.builder()
                    .id(1L)
                    .status(Loan.LoanStatus.ACTIVE)
                    .build();

            given(loanRepository.findById(1L)).willReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.disburseLoan(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already ACTIVE");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when loan not found")
        void shouldThrowExceptionWhenLoanNotFound() {
            given(loanRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.disburseLoan(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserLoans() tests")
    class GetUserLoansTests {

        @Test
        @DisplayName("Should return user loans successfully")
        void shouldReturnUserLoans() {
Loan loan = Loan.builder()
                    .id(1L)
                    .user(testUser)
                    .status(Loan.LoanStatus.ACTIVE)
                    .loanProduct(testLoanProduct)
                    .principalAmount(new BigDecimal("100000"))
                    .outstandingBalance(new BigDecimal("50000"))
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanRepository.findByUser(testUser)).willReturn(List.of(loan));

            List<LoanResponseDTO> response = loanService.getUserLoans("USR00001");

            assertThat(response).hasSize(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getUserLoans("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getLoanById() tests")
    class GetLoanByIdTests {

@Test
        @DisplayName("Should return loan successfully for owner")
        void shouldReturnLoanForOwner() {
            Loan loan = Loan.builder()
                    .id(1L)
                    .user(testUser)
                    .loanProduct(testLoanProduct)
                    .principalAmount(new BigDecimal("100000"))
                    .outstandingBalance(new BigDecimal("50000"))
                    .emiAmount(new BigDecimal("5000"))
                    .tenureMonths(24)
                    .status(Loan.LoanStatus.ACTIVE)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(loan));

            LoanResponseDTO response = loanService.getLoanById(1L, "USR00001");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not own loan")
        void shouldThrowExceptionWhenUserDoesNotOwnLoan() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(loanRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoanById(1L, "USR00001"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Loan not found or access denied");
        }
    }
}