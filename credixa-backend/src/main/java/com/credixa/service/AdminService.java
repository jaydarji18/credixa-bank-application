package com.credixa.service;

import com.credixa.dto.request.UpdateKycStatusRequestDTO;
import com.credixa.dto.request.UpdateUserStatusRequestDTO;
import com.credixa.dto.response.*;
import com.credixa.entity.*;
import com.credixa.exception.ForbiddenException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final AdminUserRepository adminUserRepository;
    private final NotificationService notificationService;
    private final LoanService loanService;

    public AdminStatsResponseDTO getAdminStats() {
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        BigDecimal transactionVolumeToday = transactionRepository.sumTotalVolumeAfter(todayStart);
        if (transactionVolumeToday == null) transactionVolumeToday = BigDecimal.ZERO;
        
        long pendingLoans = loanRepository.countByStatus(Loan.LoanStatus.APPLIED);
        long pendingKyc = userRepository.countByKycStatus(User.KycStatus.PENDING);

        return AdminStatsResponseDTO.builder()
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .transactionVolumeToday(transactionVolumeToday)
                .pendingLoans(pendingLoans)
                .pendingKyc(pendingKyc)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public Page<AdminUserListItemDTO> listUsers(String search, String statusStr, String kycStatusStr, Pageable pageable) {
        User.UserStatus status = (statusStr != null && !statusStr.isEmpty()) ? User.UserStatus.valueOf(statusStr) : null;
        User.KycStatus kycStatus = (kycStatusStr != null && !kycStatusStr.isEmpty()) ? User.KycStatus.valueOf(kycStatusStr) : null;

        return userRepository.findAllAdmin(search, status, kycStatus, pageable)
                .map(this::mapToListItemDTO);
    }

    public AdminUserDetailDTO getUserDetail(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Account> accounts = accountRepository.findByUserAndStatus(user, Account.AccountStatus.ACTIVE);
        List<Loan> loans = loanRepository.findByUser(user);

        return AdminUserDetailDTO.builder()
                .userCode(user.getUserCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .kycStatus(user.getKycStatus())
                .totalBalance(calculateTotalBalance(user))
                .createdAt(user.getCreatedAt())
                .accounts(accounts.stream().map(this::mapToAccountDTO).collect(Collectors.toList()))
                .loans(loans.stream().map(this::mapToLoanSummaryDTO).collect(Collectors.toList()))
                .lastLoginAt(user.getLastLoginAt())
                .kycRemarks(user.getKycRemarks())
                .build();
    }

    @Transactional
    public void updateUserStatus(String userCode, UpdateUserStatusRequestDTO request, String adminEmail) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Admin {} updating status of user {} to {}", adminEmail, userCode, request.getStatus());
        
        // Audit log placeholder
        log.info("[AUDIT] User Status Update: User={}, NewStatus={}, Reason={}, Admin={}", 
                userCode, request.getStatus(), request.getReason(), adminEmail);

        user.setStatus(request.getStatus());
        userRepository.save(user);

        notificationService.sendTransactionNotification(user.getId(), 
                "Account Status Updated", 
                "Your account status has been updated to " + request.getStatus() + 
                (request.getReason() != null ? ". Reason: " + request.getReason() : ""), 
                Notification.NotificationType.LOGIN_ALERT);
    }

    @Transactional
    public void updateKycStatus(String userCode, UpdateKycStatusRequestDTO request, String adminEmail) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AdminUser admin = adminUserRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (admin.getRole() != AdminUser.AdminRole.SUPER_ADMIN && 
            admin.getRole() != AdminUser.AdminRole.COMPLIANCE_OFFICER &&
            admin.getRole() != AdminUser.AdminRole.BANK_OPERATOR) {
            throw new ForbiddenException("Only SUPER_ADMIN, COMPLIANCE_OFFICER or BANK_OPERATOR can update KYC status");
        }

        user.setKycStatus(request.getKycStatus());
        user.setKycRemarks(request.getRemarks());
        userRepository.save(user);

        String title = "KYC Status Updated";
        String body = "Your KYC verification status has been updated to " + request.getKycStatus() + 
                (request.getRemarks() != null ? ". Remarks: " + request.getRemarks() : "");
        Notification.NotificationType type = request.getKycStatus() == User.KycStatus.VERIFIED ? 
                Notification.NotificationType.KYC_APPROVED : Notification.NotificationType.LOGIN_ALERT;

        notificationService.sendGlobalNotification(user, title, body, type);
        
        log.info("[AUDIT] KYC Status Update: User={}, NewStatus={}, Admin={}", userCode, request.getKycStatus(), adminEmail);
    }

    @Transactional
    public void updateUser(String userCode, com.credixa.dto.request.AdminUpdateUserRequestDTO request, String adminEmail) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Admin {} updating details of user {}", adminEmail, userCode);
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPincode() != null) user.setPincode(request.getPincode());

        userRepository.save(user);
        log.info("[AUDIT] User Updated: User={}, Admin={}", userCode, adminEmail);
    }

    @Transactional
    public void deleteUser(String userCode, String adminEmail) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Admin {} performing soft-delete on user {}", adminEmail, userCode);
        user.setDeleted(true);
        user.setStatus(User.UserStatus.CLOSED);
        userRepository.save(user);
        log.info("[AUDIT] User Deleted (Soft): User={}, Admin={}", userCode, adminEmail);
    }

    public Page<TransactionResponseDTO> getAdminTransactions(String userCode, String typeStr, String statusStr, 
                                                            BigDecimal minAmount, BigDecimal maxAmount, 
                                                            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        Transaction.TransactionType type = typeStr != null ? Transaction.TransactionType.valueOf(typeStr) : null;
        Transaction.TransactionStatus status = statusStr != null ? Transaction.TransactionStatus.valueOf(statusStr) : null;

        return transactionRepository.findAllAdmin(userCode, type, status, minAmount, maxAmount, fromDate, toDate, pageable)
                .map(this::mapToTransactionDTO);
    }

    public List<LoanResponseDTO> getPendingLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.APPLIED).stream()
                .map(this::mapToLoanSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveLoan(Long loanId, String adminEmail) {
        AdminUser admin = adminUserRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (admin.getRole() != AdminUser.AdminRole.SUPER_ADMIN && 
            admin.getRole() != AdminUser.AdminRole.BANK_MANAGER &&
            admin.getRole() != AdminUser.AdminRole.BANK_OPERATOR) {
            throw new ForbiddenException("Only SUPER_ADMIN, BANK_MANAGER or BANK_OPERATOR can approve loans");
        }

        loanService.disburseLoan(loanId);
        log.info("[AUDIT] Loan Approved: LoanID={}, Admin={}", loanId, adminEmail);
    }

    private BigDecimal calculateTotalBalance(User user) {
        return accountRepository.findByUserAndStatus(user, Account.AccountStatus.ACTIVE)
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AdminUserListItemDTO mapToListItemDTO(User u) {
        return AdminUserListItemDTO.builder()
                .userCode(u.getUserCode())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .status(u.getStatus())
                .kycStatus(u.getKycStatus())
                .totalBalance(calculateTotalBalance(u))
                .createdAt(u.getCreatedAt())
                .build();
    }

    private AccountResponseDTO mapToAccountDTO(Account a) {
        return AccountResponseDTO.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .accountType(a.getAccountType().name())
                .balance(String.format("%.2f", a.getBalance()))
                .status(a.getStatus().name())
                .isPrimary(a.isPrimary())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private LoanResponseDTO mapToLoanSummaryDTO(Loan l) {
        return LoanResponseDTO.builder()
                .id(l.getId())
                .loanNumber(l.getLoanNumber())
                .productName(l.getLoanProduct().getProductName())
                .principalAmount(l.getPrincipalAmount())
                .outstandingBalance(l.getOutstandingBalance())
                .status(l.getStatus())
                .build();
    }

    private TransactionResponseDTO mapToTransactionDTO(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .amount(t.getAmount())
                .transactionType(t.getTransactionType().name())
                .transactionStatus(t.getTransactionStatus().name())
                .initiatedAt(t.getInitiatedAt())
                .senderAccountNumber(t.getSenderAccount() != null ? t.getSenderAccount().getAccountNumber() : "N/A")
                .receiverAccountNumber(t.getReceiverAccount() != null ? t.getReceiverAccount().getAccountNumber() : "N/A")
                .description(t.getDescription())
                .build();
    }
}
