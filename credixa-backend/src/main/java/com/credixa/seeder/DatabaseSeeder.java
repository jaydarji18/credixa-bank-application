package com.credixa.seeder;

import com.credixa.entity.*;
import com.credixa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final AccountRepository accountRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final LoanProductRepository loanProductRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database for seeding demo data...");

        // 1. Seed Bank & Branch
        if (bankRepository.count() == 0) {
            Bank bank = Bank.builder()
                    .bankName("Credixa Central Bank")
                    .bankCode("CRDX")
                    .ifscPrefix("CRDX0")
                    .isActive(true)
                    .build();
            bank = bankRepository.save(bank);

            Branch branch = Branch.builder()
                    .branchName("Main Branch")
                    .ifscCode("CRDX0000001")
                    .address("BKC, Mumbai")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400051")
                    .bank(bank)
                    .isActive(true)
                    .build();
            branchRepository.save(branch);
        }

        // 2. Seed Demo User
        if (userRepository.count() == 0) {
            User user = User.builder()
                    .userCode("USR000001")
                    .firstName("Demo")
                    .lastName("User")
                    .email("demo@credixa.in")
                    .phone("9876543210")
                    .passwordHash(passwordEncoder.encode("Demo@1234"))
                    .dateOfBirth(LocalDate.of(1995, 5, 20))
                    .address("123, Green Street")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400001")
                    .aadhaarNumber("123456789012")
                    .panNumber("ABCDE1234F")
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.VERIFIED)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            user = userRepository.save(user);

            // 4. Seed Accounts (Depends on user)
            Branch branch = branchRepository.findAll().get(0);
            Account savings = Account.builder()
                    .user(user)
                    .branch(branch)
                    .accountNumber("SAV1000000001")
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("245890.00"))
                    .minimumBalance(new BigDecimal("500.00"))
                    .interestRate(new BigDecimal("4.00"))
                    .isPrimary(true)
                    .build();
            savings = accountRepository.save(savings);

            Account current = Account.builder()
                    .user(user)
                    .branch(branch)
                    .accountNumber("CUR2000000001")
                    .accountType(Account.AccountType.CURRENT)
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("82000.00"))
                    .minimumBalance(new BigDecimal("5000.00"))
                    .isPrimary(false)
                    .build();
            current = accountRepository.save(current);

            // 5. Seed Beneficiary
            Beneficiary beneficiary = Beneficiary.builder()
                    .user(user)
                    .beneficiaryName("Jane Doe")
                    .accountNumber("987654321098")
                    .ifscCode("HDFC0001234")
                    .bankName("HDFC Bank")
                    .nickname("Jane")
                    .isVerified(true)
                    .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                    .build();
            beneficiaryRepository.save(beneficiary);

            // 6. Seed Transactions (Depends on savings account)
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                Transaction.TransactionType type = i % 2 == 0 ? Transaction.TransactionType.DEPOSIT : Transaction.TransactionType.WITHDRAWAL;
                BigDecimal amount = new BigDecimal(random.nextInt(5000) + 100);
                
                Transaction transaction = Transaction.builder()
                        .referenceNumber("TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .amount(amount)
                        .netAmount(amount)
                        .transactionType(type)
                        .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                        .description("Sample " + type.name())
                        .initiatedAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .processedAt(LocalDateTime.now())
                        .category(Transaction.TransactionCategory.OTHER)
                        .build();
                
                if (type == Transaction.TransactionType.DEPOSIT) {
                    transaction.setReceiverAccount(savings);
                } else {
                    transaction.setSenderAccount(savings);
                }
                transactionRepository.save(transaction);
            }

            // 7. Seed Notifications
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .title("Welcome to Credixa Pro")
                    .body("Thank you for choosing Credixa Pro for your banking needs.")
                    .notificationType(Notification.NotificationType.LOGIN_ALERT)
                    .isRead(true)
                    .build());
        }

        // 3. Seed Demo Admin
        if (adminUserRepository.count() == 0) {
            AdminUser admin = AdminUser.builder()
                    .email("admin@credixa.in")
                    .firstName("Super")
                    .lastName("Admin")
                    .passwordHash(passwordEncoder.encode("Admin@1234"))
                    .role(AdminUser.AdminRole.SUPER_ADMIN)
                    .isActive(true)
                    .build();
            adminUserRepository.save(admin);
        }

        // 8. Seed Loan Products
        if (loanProductRepository.count() == 0) {
            loanProductRepository.save(LoanProduct.builder()
                    .productCode("LP001")
                    .productName("Ultra Personal Loan")
                    .loanType(LoanProduct.LoanType.PERSONAL_LOAN)
                    .interestRate(new BigDecimal("10.50"))
                    .minAmount(new BigDecimal("50000.00"))
                    .maxAmount(new BigDecimal("500000.00"))
                    .minTenureMonths(12)
                    .maxTenureMonths(60)
                    .isActive(true)
                    .build());

            loanProductRepository.save(LoanProduct.builder()
                    .productCode("LP002")
                    .productName("Premium Home Loan")
                    .loanType(LoanProduct.LoanType.HOME_LOAN)
                    .interestRate(new BigDecimal("8.25"))
                    .minAmount(new BigDecimal("500000.00"))
                    .maxAmount(new BigDecimal("5000000.00"))
                    .minTenureMonths(36)
                    .maxTenureMonths(240)
                    .isActive(true)
                    .build());

            loanProductRepository.save(LoanProduct.builder()
                    .productCode("LP003")
                    .productName("Elite Vehicle Loan")
                    .loanType(LoanProduct.LoanType.VEHICLE_LOAN)
                    .interestRate(new BigDecimal("9.00"))
                    .minAmount(new BigDecimal("100000.00"))
                    .maxAmount(new BigDecimal("1500000.00"))
                    .minTenureMonths(12)
                    .maxTenureMonths(84)
                    .isActive(true)
                    .build());
        }

        log.info("Database seeding completed successfully.");
    }
}
