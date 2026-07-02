package com.credixa.service;

import com.credixa.dto.request.DepositRequestDTO;
import com.credixa.dto.request.TransferRequestDTO;
import com.credixa.dto.request.WithdrawRequestDTO;
import com.credixa.dto.response.PagedTransactionResponseDTO;
import com.credixa.dto.response.StatementFileDTO;
import com.credixa.dto.response.TransactionResponseDTO;
import com.credixa.entity.*;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.InsufficientBalanceException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.AccountRepository;
import com.credixa.repository.BeneficiaryRepository;
import com.credixa.repository.TransactionRepository;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final NotificationService notificationService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void sendBalanceUpdate(Account account, Transaction transaction) {
        com.credixa.dto.response.BalanceUpdateDTO update = com.credixa.dto.response.BalanceUpdateDTO.builder()
                .accountId(account.getId())
                .newBalance(account.getBalance())
                .lastTransaction(com.credixa.dto.response.BalanceUpdateDTO.LastTransaction.builder()
                        .ref(transaction.getReferenceNumber())
                        .amount(transaction.getAmount())
                        .type(transaction.getTransactionType().name())
                        .build())
                .build();
        
        messagingTemplate.convertAndSendToUser(account.getUser().getUserCode(), "/queue/balance", update);
        log.info("Live balance update sent to user: {}", account.getUser().getUserCode());
    }

    @Transactional
    public TransactionResponseDTO deposit(String userCode, DepositRequestDTO request) {
        Account account = accountRepository.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateAccountOwnership(account, userCode);

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .receiverAccount(account)
                .amount(request.getAmount())
                .netAmount(request.getAmount())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                .description(request.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        
        sendBalanceUpdate(account, transaction);

        notificationService.sendTransactionNotification(
                account.getUser().getId(),
                "Amount Credited",
                String.format("₹%s credited to your account %s (Deposit)", request.getAmount(), account.getAccountNumber()),
                Notification.NotificationType.AMOUNT_CREDITED
        );

        TransactionResponseDTO responseDTO = mapToTransactionDTO(transaction);
        responseDTO.setSenderBalance(account.getBalance()); // Returning balance as required
        return responseDTO;
    }

    @Transactional
    public TransactionResponseDTO withdraw(String userCode, WithdrawRequestDTO request) {
        Account account = accountRepository.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateAccountOwnership(account, userCode);

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        if (account.getBalance().subtract(request.getAmount()).compareTo(account.getMinimumBalance()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance to maintain minimum balance requirement");
        }

        // Check daily limit (200,000)
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal todayTotal = transactionRepository.sumAmountByAccountAndTypeAfter(
                account.getId(), Transaction.TransactionType.WITHDRAWAL, startOfDay);
        
        if (todayTotal == null) todayTotal = BigDecimal.ZERO;

        if (todayTotal.add(request.getAmount()).compareTo(new BigDecimal("200000")) > 0) {
            throw new BadRequestException("Daily withdrawal limit of ₹200,000 exceeded");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .senderAccount(account)
                .amount(request.getAmount())
                .netAmount(request.getAmount())
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .transactionStatus(Transaction.TransactionStatus.SUCCESS)
                .description(request.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        
        sendBalanceUpdate(account, transaction);

        notificationService.sendTransactionNotification(
                account.getUser().getId(),
                "Amount Debited",
                String.format("₹%s debited from your account %s (Withdrawal)", request.getAmount(), account.getAccountNumber()),
                Notification.NotificationType.AMOUNT_DEBITED
        );

        TransactionResponseDTO responseDTO = mapToTransactionDTO(transaction);
        responseDTO.setSenderBalance(account.getBalance());
        return responseDTO;
    }

    @Transactional
    public TransactionResponseDTO transfer(String userCode, TransferRequestDTO request) {
        // Validate RTGS minimum
        if (request.getTransferType() == Transaction.TransactionType.TRANSFER_RTGS && 
            request.getAmount().compareTo(new BigDecimal("200000")) < 0) {
            throw new BadRequestException("Minimum amount for RTGS is ₹200,000");
        }

        Account senderAccount = accountRepository.findByIdWithLock(request.getSenderAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        validateAccountOwnership(senderAccount, userCode);

        if (senderAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BadRequestException("Sender account is not active");
        }

        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));

        if (!beneficiary.getUser().getUserCode().equals(userCode) || 
            beneficiary.getStatus() != Beneficiary.BeneficiaryStatus.ACTIVE) {
            throw new BadRequestException("Invalid or inactive beneficiary");
        }

        BigDecimal fee = calculateFee(request.getTransferType());
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (senderAccount.getBalance().compareTo(totalDebit) < 0) {
            this.logFailedTransaction(senderAccount, null, request.getAmount(), fee, request.getTransferType(), "Insufficient balance");
            throw new InsufficientBalanceException("Insufficient balance including fees of ₹" + fee);
        }

        try {
            Account receiverAccount = null;
            boolean isInternalIFSC = beneficiary.getIfscCode() != null && beneficiary.getIfscCode().startsWith("CRDX");
            
            if (request.getTransferType() == Transaction.TransactionType.TRANSFER_INTERNAL || isInternalIFSC) {
                receiverAccount = accountRepository.findByAccountNumber(beneficiary.getAccountNumber())
                        .orElse(null);
                
                // If explicitly requested INTERNAL but not found, throw error
                if (request.getTransferType() == Transaction.TransactionType.TRANSFER_INTERNAL && receiverAccount == null) {
                    throw new BadRequestException("Internal receiver account not found in system");
                }
            }

            Transaction transaction = Transaction.builder()
                    .referenceNumber(generateReferenceNumber())
                    .senderAccount(senderAccount)
                    .receiverAccount(receiverAccount)
                    .beneficiary(beneficiary)
                    .amount(request.getAmount())
                    .fee(fee)
                    .netAmount(totalDebit)
                    .transactionType(request.getTransferType())
                    .transactionStatus(Transaction.TransactionStatus.PENDING)
                    .description(request.getRemarks())
                    .build();

            transaction = transactionRepository.save(transaction);

            // Deduct total amount (amount + fee) from sender
            senderAccount.setBalance(senderAccount.getBalance().subtract(totalDebit));
            accountRepository.save(senderAccount);

            if (receiverAccount != null) {
                receiverAccount.setBalance(receiverAccount.getBalance().add(request.getAmount()));
                accountRepository.save(receiverAccount);
            }

            // Mark as SUCCESS
            transaction.setTransactionStatus(Transaction.TransactionStatus.SUCCESS);
            transaction.setProcessedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            sendBalanceUpdate(senderAccount, transaction);
            if (receiverAccount != null) {
                sendBalanceUpdate(receiverAccount, transaction);
            }

            notificationService.sendTransactionNotification(
                    senderAccount.getUser().getId(),
                    "Amount Debited",
                    String.format("₹%s transferred to %s (Ref: %s)", 
                        request.getAmount(), beneficiary.getBeneficiaryName(), transaction.getReferenceNumber()),
                    Notification.NotificationType.AMOUNT_DEBITED
            );

            if (receiverAccount != null) {
                notificationService.sendTransactionNotification(
                        receiverAccount.getUser().getId(),
                        "Amount Credited",
                        String.format("₹%s received from %s (Ref: %s)", 
                            request.getAmount(), senderAccount.getUser().getFirstName(), transaction.getReferenceNumber()),
                        Notification.NotificationType.AMOUNT_CREDITED
                );
            }

            TransactionResponseDTO responseDTO = mapToTransactionDTO(transaction);
            responseDTO.setSenderBalance(senderAccount.getBalance());
            return responseDTO;

        } catch (Exception ex) {
            log.error("Transfer failed: {}", ex.getMessage());
            this.logFailedTransaction(senderAccount, null, request.getAmount(), fee, request.getTransferType(), ex.getMessage());
            throw ex;
        }
    }

    public PagedTransactionResponseDTO getTransactionHistory(
            String userCode, Long accountId, Transaction.TransactionType type,
            Transaction.TransactionStatus status, Transaction.TransactionCategory category,
            LocalDateTime fromDate, LocalDateTime toDate, String search, Pageable pageable) {
        
        User user = findUserByCode(userCode);
        Long userId = user.getId();

        Page<Transaction> page = transactionRepository.findByUserWithFilters(
                userId, accountId, type, status, category, fromDate, toDate, search, pageable);

        List<TransactionResponseDTO> content = page.getContent().stream()
                .map(this::mapToTransactionDTO)
                .collect(Collectors.toList());

        return PagedTransactionResponseDTO.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public StatementFileDTO downloadStatement(
            String userCode, Long accountId, LocalDateTime fromDate, LocalDateTime toDate, 
            Transaction.TransactionType type, Transaction.TransactionStatus status, String search, String format) {

        // Sanitize inputs - empty strings should be treated as null
        String cleanSearch = (search == null || search.trim().isEmpty()) ? null : search;
        
        // DEFAULT DATE LOGIC: If not provided, use April 1st, 2026 to Today
        LocalDateTime finalFromDate = fromDate;
        if (finalFromDate == null) {
            finalFromDate = LocalDateTime.of(2026, 4, 1, 0, 0);
        }
        
        LocalDateTime finalToDate = toDate;
        if (finalToDate == null) {
            finalToDate = LocalDateTime.now();
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        if (!account.getUser().getUserCode().equals(userCode)) {
            throw new BadRequestException("Permission denied");
        }

        // ULTIMATE NATIVE QUERY SOLUTION (No Stream API)
        String typeStr = (type != null) ? type.name() : null;
        String statusStr = (status != null) ? status.name() : null;

        List<Transaction> filteredTransactions = transactionRepository.findByUserWithFiltersNative(
                accountId, typeStr, statusStr, finalFromDate, finalToDate, cleanSearch);

        if (filteredTransactions == null) {
            filteredTransactions = new java.util.ArrayList<>();
        }

        String period = finalFromDate.toLocalDate().toString() + "_" + finalToDate.toLocalDate().toString();
        
        if ("xlsx".equalsIgnoreCase(format)) {
            return generateExcelStatement(account, filteredTransactions, period);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdfStatement(account, filteredTransactions, period);
        } else {
            return generateCsvStatement(account, filteredTransactions, period);
        }
    }

    private StatementFileDTO generateCsvStatement(Account account, List<Transaction> transactions, String period) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Reference,Type,Amount,Fee,Status,Description,Date");
            for (Transaction t : transactions) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        t.getReferenceNumber(),
                        t.getTransactionType(),
                        t.getAmount(),
                        t.getFee(),
                        t.getTransactionStatus(),
                        t.getDescription() != null ? t.getDescription().replace(",", ";") : "",
                        t.getInitiatedAt());
            }
        }
        String filename = String.format("statement_%s_%s.csv", account.getAccountNumber(), period);
        return new StatementFileDTO(new ByteArrayResource(out.toByteArray()), filename);
    }

    private StatementFileDTO generatePdfStatement(Account account, List<Transaction> transactions, String period) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Font Definitions
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLACK);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            com.lowagie.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);

            // Bank Branding
            Paragraph brand = new Paragraph("CREDIXA PRO", titleFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);
            
            Paragraph subBrand = new Paragraph("Digital Banking Solutions", FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            subBrand.setAlignment(Element.ALIGN_CENTER);
            subBrand.setSpacingAfter(20);
            document.add(subBrand);

            // Statement Details
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);
            infoTable.setSpacingAfter(20);

            infoTable.addCell(getPdfCell("Account Number:", subTitleFont, false));
            infoTable.addCell(getPdfCell(account.getAccountNumber(), bodyFont, false));
            infoTable.addCell(getPdfCell("Account Type:", subTitleFont, false));
            infoTable.addCell(getPdfCell(account.getAccountType().toString(), bodyFont, false));
            infoTable.addCell(getPdfCell("Current Balance:", subTitleFont, false));
            infoTable.addCell(getPdfCell("INR " + account.getBalance().toString(), bodyFont, false));
            infoTable.addCell(getPdfCell("Statement Period:", subTitleFont, false));
            infoTable.addCell(getPdfCell(period.replace("_", " to "), bodyFont, false));

            document.add(infoTable);

            // Transactions Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 3f, 3f, 2f, 4f});

            // Table Headers
            String[] headers = {"Date", "Reference", "Type", "From", "To", "Amount", "Description"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(26, 35, 126)); // Deep Indigo
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(Color.WHITE);
                table.addCell(cell);
            }

            // Table Body
            for (Transaction t : transactions){
                String dateStr = t.getInitiatedAt() != null ? t.getInitiatedAt().toLocalDate().toString() : "";
                log.warn(dateStr);
                String ref = t.getReferenceNumber() != null ? t.getReferenceNumber().substring(0, Math.min(8, t.getReferenceNumber().length())) : "";
                String typeStr = t.getTransactionType() != null ? t.getTransactionType().toString() : "";
                String fromAcc = t.getSenderAccount() != null ? t.getSenderAccount().getAccountNumber() : "External/Cash";
                String toAcc = t.getReceiverAccount() != null ? t.getReceiverAccount().getAccountNumber() : "External/Cash";
                String amountStr = t.getAmount() != null ? t.getAmount().toString() : "0.00";
                String descStr = t.getDescription() != null ? t.getDescription() : "";

                table.addCell(getPdfCell(dateStr, bodyFont, true));
                table.addCell(getPdfCell(ref, bodyFont, true));
                table.addCell(getPdfCell(typeStr, bodyFont, true));
                table.addCell(getPdfCell(fromAcc, bodyFont, true));
                table.addCell(getPdfCell(toAcc, bodyFont, true));
                table.addCell(getPdfCell(amountStr, bodyFont, true));
                table.addCell(getPdfCell(descStr, bodyFont, false));
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("\n\nThis is a computer-generated document and does not require a physical signature.", 
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            String filename = String.format("statement_%s_%s.pdf", account.getAccountNumber(), period);
            return new StatementFileDTO(new ByteArrayResource(out.toByteArray()), filename);
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("Failed to generate PDF statement", e);
        }
    }

    private PdfPCell getPdfCell(String text, com.lowagie.text.Font font, boolean center) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        if (center) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private StatementFileDTO generateExcelStatement(Account account, List<Transaction> transactions, String period) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create Headers
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Reference", "Type", "From Account", "To Account", "Amount", "Fee", "Status", "Description"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill Data
            int rowIdx = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getInitiatedAt() != null ? t.getInitiatedAt().toString() : "");
                row.createCell(1).setCellValue(t.getReferenceNumber() != null ? t.getReferenceNumber() : "");
                row.createCell(2).setCellValue(t.getTransactionType() != null ? t.getTransactionType().toString() : "");
                row.createCell(3).setCellValue(t.getSenderAccount() != null ? t.getSenderAccount().getAccountNumber() : "N/A");
                row.createCell(4).setCellValue(t.getReceiverAccount() != null ? t.getReceiverAccount().getAccountNumber() : "N/A");
                row.createCell(5).setCellValue(t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
                row.createCell(6).setCellValue(t.getFee() != null ? t.getFee().doubleValue() : 0.0);
                row.createCell(7).setCellValue(t.getTransactionStatus() != null ? t.getTransactionStatus().toString() : "");
                row.createCell(8).setCellValue(t.getDescription() != null ? t.getDescription() : "");
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            String filename = String.format("statement_%s_%s.xlsx", account.getAccountNumber(), period);
            return new StatementFileDTO(new ByteArrayResource(out.toByteArray()), filename);
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new RuntimeException("Failed to generate Excel statement", e);
        }
    }

    public TransactionResponseDTO getTransactionByReference(String referenceNumber, String userCode) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Validate ownership (sender or receiver)
        boolean isSender = transaction.getSenderAccount() != null && 
                          transaction.getSenderAccount().getUser().getUserCode().equals(userCode);
        boolean isReceiver = transaction.getReceiverAccount() != null && 
                            transaction.getReceiverAccount().getUser().getUserCode().equals(userCode);

        if (!isSender && !isReceiver) {
            throw new BadRequestException("Permission denied for this transaction");
        }

        return mapToTransactionDTO(transaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedTransaction(Account sender, Account receiver, BigDecimal amount, BigDecimal fee, 
                                   Transaction.TransactionType type, String reason) {
        Transaction transaction = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(amount)
                .fee(fee)
                .netAmount(amount.add(fee != null ? fee : BigDecimal.ZERO))
                .transactionType(type)
                .transactionStatus(Transaction.TransactionStatus.FAILED)
                .description("FAILED: " + reason)
                .build();
        transactionRepository.save(transaction);
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateAccountOwnership(Account account, String userCode) {
        if (!account.getUser().getUserCode().equals(userCode)) {
            throw new BadRequestException("Permission denied for this account");
        }
    }

    private BigDecimal calculateFee(Transaction.TransactionType type) {
        return switch (type) {
            case TRANSFER_NEFT -> new BigDecimal("5");
            case TRANSFER_RTGS -> new BigDecimal("25");
            case TRANSFER_IMPS -> new BigDecimal("10");
            case TRANSFER_UPI -> BigDecimal.ZERO;
            case TRANSFER_INTERNAL -> BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    private String generateReferenceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(9000) + 1000;
        return "TXN" + timestamp + random;
    }

    private TransactionResponseDTO mapToTransactionDTO(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .transactionType(t.getTransactionType().name())
                .amount(t.getAmount())
                .fee(t.getFee())
                .netAmount(t.getNetAmount())
                .transactionStatus(t.getTransactionStatus().name())
                .description(t.getDescription())
                .category(t.getCategory() != null ? t.getCategory().name() : "OTHER")
                .initiatedAt(t.getInitiatedAt())
                .processedAt(t.getProcessedAt())
                .senderAccountNumber(t.getSenderAccount() != null ? t.getSenderAccount().getAccountNumber() : null)
                .receiverAccountNumber(t.getReceiverAccount() != null ? t.getReceiverAccount().getAccountNumber() : null)
                .beneficiaryName(t.getBeneficiary() != null ? t.getBeneficiary().getBeneficiaryName() : null)
                .build();
    }
}
