package com.credixa.service;

import com.credixa.dto.request.UpdateKycStatusRequestDTO;
import com.credixa.dto.request.UpdateUserStatusRequestDTO;
import com.credixa.dto.request.AdminUpdateUserRequestDTO;
import com.credixa.dto.response.*;
import com.credixa.entity.*;
import com.credixa.exception.ForbiddenException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private AdminService adminService;

    private AdminUser testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = AdminUser.builder()
                .id(1L)
                .email("admin@credixa.com")
                .firstName("Admin")
                .lastName("User")
                .role(AdminUser.AdminRole.SUPER_ADMIN)
                .build();
    }

    @Nested
    @DisplayName("getAdminStats() tests")
    class GetAdminStatsTests {

        @Test
        @DisplayName("Should return admin stats successfully")
        void shouldReturnAdminStats() {
            given(userRepository.count()).willReturn(1000L);
            given(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(50L);
            given(transactionRepository.sumTotalVolumeAfter(any(LocalDateTime.class)))
                    .willReturn(new BigDecimal("5000000"));
            given(loanRepository.countByStatus(Loan.LoanStatus.APPLIED)).willReturn(25L);
            given(userRepository.countByKycStatus(User.KycStatus.PENDING)).willReturn(10L);

            AdminStatsResponseDTO response = adminService.getAdminStats();

            assertThat(response).isNotNull();
            assertThat(response.getTotalUsers()).isEqualTo(1000);
            assertThat(response.getNewUsersToday()).isEqualTo(50);
            assertThat(response.getPendingLoans()).isEqualTo(25);
            assertThat(response.getPendingKyc()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should handle null transaction volume gracefully")
        void shouldHandleNullTransactionVolume() {
            given(userRepository.count()).willReturn(1000L);
            given(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(50L);
            given(transactionRepository.sumTotalVolumeAfter(any(LocalDateTime.class))).willReturn(null);
            given(loanRepository.countByStatus(any())).willReturn(0L);
            given(userRepository.countByKycStatus(any())).willReturn(0L);

            AdminStatsResponseDTO response = adminService.getAdminStats();

            assertThat(response).isNotNull();
            assertThat(response.getTransactionVolumeToday()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("listUsers() tests")
    class ListUsersTests {

        @Test
        @DisplayName("Should list users with search filter")
        void shouldListUsersWithSearch() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("9876543210")
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.VERIFIED)
                    .build();

            given(userRepository.findAllAdmin(anyString(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(user)));
            var response = adminService.listUsers("john", "ACTIVE", "VERIFIED", Pageable.ofSize(10));

            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should list users with null status filters")
        void shouldListUsersWithNullFilters() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.VERIFIED)
                    .build();

given(userRepository.findAllAdmin(isNull(), any(), any(), any()))
                     .willReturn(new PageImpl<>(List.of(user)));
             var response = adminService.listUsers(null, null, null, Pageable.ofSize(10));

            assertThat(response.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getUserDetail() tests")
    class GetUserDetailTests {

        @Test
        @DisplayName("Should get user detail successfully")
        void shouldGetUserDetail() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("9876543210")
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.VERIFIED)
                    .build();

Account account = Account.builder()
                     .id(1L)
                     .user(user)
                     .status(Account.AccountStatus.ACTIVE)
                     .balance(new BigDecimal("50000"))
                     .accountType(Account.AccountType.SAVINGS)
                     .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(user));
            given(accountRepository.findByUserAndStatus(user, Account.AccountStatus.ACTIVE))
                    .willReturn(List.of(account));
            given(loanRepository.findByUser(user)).willReturn(List.of());

            AdminUserDetailDTO response = adminService.getUserDetail("USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getUserCode()).isEqualTo("USR00001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserDetail("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUserStatus() tests")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should update user status successfully")
        void shouldUpdateUserStatus() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            UpdateUserStatusRequestDTO request = UpdateUserStatusRequestDTO.builder()
                    .status(User.UserStatus.SUSPENDED)
                    .reason("Security concern")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(user));

            adminService.updateUserStatus("USR00001", request, "admin@credixa.com");

            verify(userRepository).save(argThat(u -> 
                    u.getStatus() == User.UserStatus.SUSPENDED
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent user status")
        void shouldThrowExceptionWhenUpdatingNonExistentUserStatus() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserStatus("USR99999", 
                    new UpdateUserStatusRequestDTO(), "admin@credixa.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateKycStatus() tests")
    class UpdateKycStatusTests {

        @Test
        @DisplayName("Should update KYC status successfully for authorized admin")
        void shouldUpdateKycStatusForAuthorizedAdmin() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .kycStatus(User.KycStatus.PENDING)
                    .build();

            UpdateKycStatusRequestDTO request = UpdateKycStatusRequestDTO.builder()
                    .kycStatus(User.KycStatus.VERIFIED)
                    .remarks("Document verified")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(user));
            given(adminUserRepository.findByEmail("admin@credixa.com")).willReturn(Optional.of(testAdmin));

            adminService.updateKycStatus("USR00001", request, "admin@credixa.com");

            verify(userRepository).save(argThat(u -> 
                    u.getKycStatus() == User.KycStatus.VERIFIED
            ));
        }

        @Test
        @DisplayName("Should throw ForbiddenException for unauthorized admin role")
        void shouldThrowExceptionForUnauthorizedAdmin() {
            AdminUser lowLevelAdmin = AdminUser.builder()
                    .id(2L)
                    .email("lowadmin@credixa.com")
                    .role(AdminUser.AdminRole.SUPPORT_AGENT)
                    .build();

            UpdateKycStatusRequestDTO request = UpdateKycStatusRequestDTO.builder()
                    .kycStatus(User.KycStatus.VERIFIED)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(new User()));
            given(adminUserRepository.findByEmail("lowadmin@credixa.com")).willReturn(Optional.of(lowLevelAdmin));

            assertThatThrownBy(() -> adminService.updateKycStatus("USR00001", request, "lowadmin@credixa.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only SUPER_ADMIN, COMPLIANCE_OFFICER or BANK_OPERATOR");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when admin not found")
        void shouldThrowExceptionWhenAdminNotFound() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(new User()));
            given(adminUserRepository.findByEmail("nonexistent@credixa.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateKycStatus("USR00001", 
                    new UpdateKycStatusRequestDTO(), "nonexistent@credixa.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Admin not found");
        }
    }

    @Nested
    @DisplayName("updateUser() tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user details successfully")
        void shouldUpdateUser() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            AdminUpdateUserRequestDTO request = AdminUpdateUserRequestDTO.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .phone("9999999999")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(user));

            adminService.updateUser("USR00001", request, "admin@credixa.com");

            verify(userRepository).save(argThat(u -> 
                    u.getFirstName().equals("Jane") &&
                    u.getEmail().equals("jane@example.com")
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
        void shouldThrowExceptionWhenUpdatingNonExistentUser() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUser("USR99999", 
                    new AdminUpdateUserRequestDTO(), "admin@credixa.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser() tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should soft delete user successfully")
        void shouldSoftDeleteUser() {
            User user = User.builder()
                    .id(1L)
                    .userCode("USR00001")
                    .status(User.UserStatus.ACTIVE)
                    .isDeleted(false)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(user));

            adminService.deleteUser("USR00001", "admin@credixa.com");

            verify(userRepository).save(argThat(u -> 
                    u.isDeleted() == true &&
                    u.getStatus() == User.UserStatus.CLOSED
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser("USR99999", "admin@credixa.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPendingLoans() tests")
    class GetPendingLoansTests {

        @Test
        @DisplayName("Should return pending loans")
        void shouldReturnPendingLoans() {
Loan loan = Loan.builder()
                     .id(1L)
                     .loanProduct(LoanProduct.builder().productName("Personal Loan").build())
                     .status(Loan.LoanStatus.APPLIED)
                     .build();

            given(loanRepository.findByStatus(Loan.LoanStatus.APPLIED)).willReturn(List.of(loan));

            List<LoanResponseDTO> response = adminService.getPendingLoans();

            assertThat(response).hasSize(1);
        }
    }

    @Nested
    @DisplayName("approveLoan() tests")
    class ApproveLoanTests {

        @Test
        @DisplayName("Should approve loan for authorized admin")
        void shouldApproveLoanForAuthorizedAdmin() {
            given(adminUserRepository.findByEmail("admin@credixa.com")).willReturn(Optional.of(testAdmin));

            adminService.approveLoan(1L, "admin@credixa.com");

            verify(loanService).disburseLoan(1L);
        }

        @Test
        @DisplayName("Should throw ForbiddenException for unauthorized admin role")
        void shouldThrowExceptionForUnauthorizedAdminRole() {
            AdminUser teller = AdminUser.builder()
                    .id(2L)
                    .email("teller@credixa.com")
                    .role(AdminUser.AdminRole.TELLER)
                    .build();

            given(adminUserRepository.findByEmail("teller@credixa.com")).willReturn(Optional.of(teller));

            assertThatThrownBy(() -> adminService.approveLoan(1L, "teller@credixa.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only SUPER_ADMIN, BANK_MANAGER or BANK_OPERATOR");
        }
    }
}