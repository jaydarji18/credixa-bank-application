package com.credixa.repository;

import com.credixa.entity.Loan;
import com.credixa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser(User user);
    Optional<Loan> findByIdAndUser(Long id, User user);
    List<Loan> findByUserAndStatus(User user, Loan.LoanStatus status);
    List<Loan> findByStatus(Loan.LoanStatus status);
    long countByStatus(Loan.LoanStatus status);
}
