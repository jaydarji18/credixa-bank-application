package com.credixa.repository;

import com.credixa.entity.Account;
import com.credixa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserAndStatus(User user, Account.AccountStatus status);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserIdOrderByIsPrimaryDesc(Long userId);
    Optional<Account> findByIdAndUser(Long id, User user);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
