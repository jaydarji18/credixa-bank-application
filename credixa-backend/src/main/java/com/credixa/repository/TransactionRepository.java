package com.credixa.repository;

import com.credixa.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN t.senderAccount sa " +
           "LEFT JOIN t.receiverAccount ra " +
           "WHERE (sa.user.id = :userId OR ra.user.id = :userId) " +
           "AND (:accountId IS NULL OR sa.id = :accountId OR ra.id = :accountId) " +
           "AND (:type IS NULL OR t.transactionType = :type) " +
           "AND (:status IS NULL OR t.transactionStatus = :status) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:startDate IS NULL OR t.initiatedAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.initiatedAt <= :endDate) " +
           "AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Transaction> findByUserWithFilters(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("type") Transaction.TransactionType type,
            @Param("status") Transaction.TransactionStatus status,
            @Param("category") Transaction.TransactionCategory category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionStatus = 'SUCCESS' " +
           "AND t.initiatedAt >= :startDate")
    BigDecimal sumTotalVolumeAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN t.senderAccount sa " +
           "LEFT JOIN t.receiverAccount ra " +
           "WHERE (:userCode IS NULL OR sa.user.userCode = :userCode OR ra.user.userCode = :userCode) AND " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:status IS NULL OR t.transactionStatus = :status) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:fromDate IS NULL OR t.initiatedAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.initiatedAt <= :toDate)")
    Page<Transaction> findAllAdmin(@Param("userCode") String userCode,
                                  @Param("type") Transaction.TransactionType type,
                                  @Param("status") Transaction.TransactionStatus status,
                                  @Param("minAmount") BigDecimal minAmount,
                                  @Param("maxAmount") BigDecimal maxAmount,
                                  @Param("fromDate") LocalDateTime fromDate,
                                  @Param("toDate") LocalDateTime toDate,
                                  Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.receiverAccount.user.id = :userId " +
           "AND t.transactionStatus = 'SUCCESS' " +
           "AND t.transactionType IN :types " +
           "AND t.initiatedAt >= :startDate")
    BigDecimal sumIncomeForMonth(
            @Param("userId") Long userId,
            @Param("types") Collection<Transaction.TransactionType> types,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.senderAccount.user.id = :userId " +
           "AND t.transactionStatus = 'SUCCESS' " +
           "AND t.transactionType IN :types " +
           "AND t.initiatedAt >= :startDate")
    BigDecimal sumExpensesForMonth(
            @Param("userId") Long userId,
            @Param("types") Collection<Transaction.TransactionType> types,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT CAST(t.initiatedAt AS date) as date, SUM(t.amount) as amount FROM Transaction t " +
           "WHERE t.senderAccount.user.id = :userId " +
           "AND t.transactionStatus = 'SUCCESS' " +
           "AND t.transactionType IN :types " +
           "AND t.initiatedAt >= :startDate " +
           "GROUP BY CAST(t.initiatedAt AS date) " +
           "ORDER BY date ASC")
    List<Object[]> sumDailyExpenses(
            @Param("userId") Long userId,
            @Param("types") Collection<Transaction.TransactionType> types,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.senderAccount.id = :accountId " +
           "AND t.transactionType = :type " +
           "AND t.transactionStatus = 'SUCCESS' " +
           "AND t.initiatedAt >= :startDate")
    BigDecimal sumAmountByAccountAndTypeAfter(
            @Param("accountId") Long accountId,
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDateTime startDate
    );

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query(value = "SELECT * FROM transactions t " +
           "WHERE (t.sender_account_id = :accountId OR t.receiver_account_id = :accountId) " +
           "AND (:startDate IS NULL OR t.initiated_at >= :startDate) " +
           "AND (:endDate IS NULL OR t.initiated_at <= :endDate) " +
           "AND (:type IS NULL OR t.transaction_type = :type) " +
           "AND (:status IS NULL OR t.transaction_status = :status) " +
           "AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.reference_number) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.initiated_at DESC", nativeQuery = true)
    List<Transaction> findByUserWithFiltersNative(
            @Param("accountId") Long accountId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search
    );
}
