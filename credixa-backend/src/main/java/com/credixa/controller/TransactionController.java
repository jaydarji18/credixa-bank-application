package com.credixa.controller;

import com.credixa.dto.request.DepositRequestDTO;
import com.credixa.dto.request.TransferRequestDTO;
import com.credixa.dto.request.WithdrawRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.PagedTransactionResponseDTO;
import com.credixa.dto.response.StatementFileDTO;
import com.credixa.dto.response.TransactionResponseDTO;
import com.credixa.entity.Transaction;
import com.credixa.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Endpoints for financial movements including deposits, withdrawals, and transfers")
public class TransactionController extends BaseController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get transaction history", description = "Retrieves a paginated and filtered history of transactions for the user")
    public ResponseEntity<ApiResponse<PagedTransactionResponseDTO>> getHistory(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionStatus status,
            @RequestParam(required = false) Transaction.TransactionCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "initiatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PagedTransactionResponseDTO history = transactionService.getTransactionHistory(
                getCurrentUserCode(), accountId, type, status, category, fromDate, toDate, search, pageable);

        return ResponseEntity.ok(ApiResponse.success(history, "Transaction history fetched successfully"));
    }

    @GetMapping("/{referenceNumber}")
    @Operation(summary = "Get transaction details", description = "Retrieves specific details for a transaction by its reference number")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransactionDetail(
            @PathVariable String referenceNumber) {

        TransactionResponseDTO response = transactionService.getTransactionByReference(
                referenceNumber, getCurrentUserCode());

        return ResponseEntity.ok(ApiResponse.success(response, "Transaction details fetched successfully"));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds", description = "Credits the specified amount to a user's account")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> deposit(
            @Valid @RequestBody DepositRequestDTO request) {

        TransactionResponseDTO response = transactionService.deposit(getCurrentUserCode(), request);

        return new ResponseEntity<>(
                ApiResponse.success(response, "Deposit successful"),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds", description = "Debits the specified amount from a user's account, subject to limits")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> withdraw(
            @Valid @RequestBody WithdrawRequestDTO request) {

        TransactionResponseDTO response = transactionService.withdraw(getCurrentUserCode(), request);

        return new ResponseEntity<>(
                ApiResponse.success(response, "Withdrawal successful"),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds", description = "Initiates an internal or external transfer to a beneficiary")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> transfer(
            @Valid @RequestBody TransferRequestDTO request) {

        TransactionResponseDTO response = transactionService.transfer(getCurrentUserCode(), request);

        return new ResponseEntity<>(
                ApiResponse.success(response, "Transfer successful"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/statement")
    @Operation(summary = "Download account statement", description = "Generates and downloads a CSV statement for a given period")
    public ResponseEntity<Resource> downloadStatement(
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "csv") String format) {

        log.info("Downloading {} statement for account {} from {} to {}", format, accountId, fromDate, toDate);

        StatementFileDTO statementFile = transactionService.downloadStatement(
                getCurrentUserCode(), accountId, fromDate, toDate, type, status, search, format);

        String contentType = "text/csv";
        if (format.equalsIgnoreCase("xlsx")) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (format.equalsIgnoreCase("pdf")) {
            contentType = "application/pdf";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + statementFile.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(statementFile.getResource());
    }
}