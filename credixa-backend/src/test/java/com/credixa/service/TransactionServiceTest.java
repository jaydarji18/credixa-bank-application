package com.credixa.service;

import com.credixa.dto.request.DepositRequestDTO;
import com.credixa.dto.request.TransferRequestDTO;
import com.credixa.dto.request.WithdrawRequestDTO;
import com.credixa.dto.response.PagedTransactionResponseDTO;
import com.credixa.dto.response.StatementFileDTO;
import com.credixa.dto.response.TransactionResponseDTO;
import com.credixa.entity.Account;
import com.credixa.entity.Beneficiary;
import com.credixa.entity.Transaction;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.InsufficientBalanceException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.AccountRepository;
import com.credixa.repository.BeneficiaryRepository;
import com.credixa.repository.TransactionRepository;
import com.credixa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;
    private Beneficiary testBeneficiary;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(User.UserStatus.ACTIVE)
                .build();

        testAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountType(Account.AccountType.SAVINGS)
                .status(Account.AccountStatus.ACTIVE)
                .balance(new BigDecimal("10000"))
                .minimumBalance(new BigDecimal("500"))
                .user(testUser)
                .build();

        testBeneficiary = Beneficiary.builder()
                .id(1L)
                .beneficiaryName("Jane Doe")
                .accountNumber("0987654321")
                .ifscCode("CRDX0002")
                .user(testUser)
                .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                .isVerified(true)
                .build();
    }

    @Nested
    @DisplayName("deposit() tests")
    class DepositTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            DepositRequestDTO request = new DepositRequestDTO();
            request.setAccountId(999L);
            request.setAmount(new BigDecimal("5000"));

            given(accountRepository.findByIdWithLock(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deposit("USR00001", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found");
        }

        @Test
        @DisplayName("Should throw BadRequestException when account is not active")
        void shouldThrowExceptionWhenAccountNotActive() {
            testAccount.setStatus(Account.AccountStatus.FROZEN);

            DepositRequestDTO request = new DepositRequestDTO();
            request.setAccountId(1L);
            request.setAmount(new BigDecimal("5000"));

            given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> transactionService.deposit("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Account is not active");
        }
    }

    @Nested
    @DisplayName("withdraw() tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should throw InsufficientBalanceException when balance below minimum")
        void shouldThrowExceptionWhenInsufficientBalance() {
            Account lowBalanceAccount = Account.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .user(testUser)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("600"))
                    .minimumBalance(new BigDecimal("500"))
                    .build();

            WithdrawRequestDTO request = new WithdrawRequestDTO();
            request.setAccountId(1L);
            request.setAmount(new BigDecimal("200"));

            given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(lowBalanceAccount));

            assertThatThrownBy(() -> transactionService.withdraw("USR00001", request))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when daily withdrawal limit exceeded")
        void shouldThrowExceptionWhenDailyLimitExceeded() {
            Account highBalanceAccount = Account.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .user(testUser)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("500000"))
                    .minimumBalance(BigDecimal.ZERO)
                    .build();

            WithdrawRequestDTO request = new WithdrawRequestDTO();
            request.setAccountId(1L);
            request.setAmount(new BigDecimal("200000"));

            given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(highBalanceAccount));
            given(transactionRepository.sumAmountByAccountAndTypeAfter(anyLong(), any(), any()))
                    .willReturn(new BigDecimal("150000"));

            assertThatThrownBy(() -> transactionService.withdraw("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Daily withdrawal limit");
        }
    }

    @Nested
    @DisplayName("transfer() tests")
    class TransferTests {

        @Test
        @DisplayName("Should throw BadRequestException when RTGS amount below minimum")
        void shouldThrowExceptionWhenRtgsBelowMinimum() {
            TransferRequestDTO request = new TransferRequestDTO();
            request.setSenderAccountId(1L);
            request.setBeneficiaryId(1L);
            request.setAmount(new BigDecimal("100000"));
            request.setTransferType(Transaction.TransactionType.TRANSFER_RTGS);

            assertThatThrownBy(() -> transactionService.transfer("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Minimum amount for RTGS is ₹200,000");
        }

        @Test
        @DisplayName("Should throw BadRequestException when beneficiary is inactive")
        void shouldThrowExceptionWhenBeneficiaryInactive() {
            testBeneficiary.setStatus(Beneficiary.BeneficiaryStatus.INACTIVE);

            TransferRequestDTO request = new TransferRequestDTO();
            request.setSenderAccountId(1L);
            request.setBeneficiaryId(1L);
            request.setAmount(new BigDecimal("5000"));
            request.setTransferType(Transaction.TransactionType.TRANSFER_NEFT);

            given(accountRepository.findByIdWithLock(anyLong())).willReturn(Optional.of(testAccount));
            given(beneficiaryRepository.findById(1L)).willReturn(Optional.of(testBeneficiary));

            assertThatThrownBy(() -> transactionService.transfer("USR00001", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid or inactive beneficiary");
        }
    }

    @Nested
    @DisplayName("getTransactionHistory() tests")
    class TransactionHistoryTests {

        @Test
        @DisplayName("Should return transaction history successfully")
        void shouldReturnTransactionHistory() {
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .referenceNumber("TXN202401151200001234")
                    .transactionType(Transaction.TransactionType.DEPOSIT)
                    .amount(new BigDecimal("5000"))
                    .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                    .initiatedAt(LocalDateTime.now())
                    .build();

            Page<Transaction> page = new PageImpl<>(List.of(transaction));
            
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(transactionRepository.findByUserWithFilters(anyLong(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(page);

            PagedTransactionResponseDTO response = transactionService.getTransactionHistory(
                    "USR00001", null, null, null, null, null, null, null, Pageable.ofSize(10));

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found for history")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionHistory(
                    "USR99999", null, null, null, null, null, null, null, Pageable.ofSize(10)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("downloadStatement() tests")
    class DownloadStatementTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found for statement")
        void shouldThrowExceptionWhenAccountNotFound() {
            given(accountRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.downloadStatement(
                    "USR00001", 999L, null, null, null, null, null, "csv"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when user does not own account for statement")
        void shouldThrowExceptionWhenUserDoesNotOwnAccountForStatement() {
            User otherUser = User.builder().id(2L).userCode("USR00002").build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(
                    Account.builder().id(1L).user(otherUser).build()));

            assertThatThrownBy(() -> transactionService.downloadStatement(
                    "USR00001", 1L, null, null, null, null, null, "csv"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Permission denied");
        }
    }

    @Nested
    @DisplayName("getTransactionByReference() tests")
    class GetTransactionByReferenceTests {

        @Test
        @DisplayName("Should throw BadRequestException when user does not own transaction")
        void shouldThrowExceptionWhenUserDoesNotOwnTransaction() {
            User otherUser = User.builder().id(2L).userCode("USR00002").build();
            Account otherAccount = Account.builder()
                    .id(2L)
                    .accountNumber("0987654321")
                    .user(otherUser)
                    .status(Account.AccountStatus.ACTIVE)
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .referenceNumber("TXN123456")
                    .senderAccount(otherAccount)
                    .transactionType(Transaction.TransactionType.TRANSFER_NEFT)
                    .amount(new BigDecimal("5000"))
                    .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                    .initiatedAt(LocalDateTime.now())
                    .build();

            given(transactionRepository.findByReferenceNumber("TXN123456")).willReturn(Optional.of(transaction));

            assertThatThrownBy(() -> transactionService.getTransactionByReference("TXN123456", "USR00001"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Permission denied for this transaction");
        }
    }
}