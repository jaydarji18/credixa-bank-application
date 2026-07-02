package com.credixa.repository;

import com.credixa.entity.Beneficiary;
import com.credixa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByUserAndAccountNumberAndIfscCode(User user, String accountNumber, String ifscCode);
    List<Beneficiary> findByUserAndStatus(User user, Beneficiary.BeneficiaryStatus status);
    Optional<Beneficiary> findByIdAndUser(Long id, User user);
    List<Beneficiary> findByUserIdAndStatus(Long userId, Beneficiary.BeneficiaryStatus status);
    Optional<Beneficiary> findByUserIdAndId(Long userId, Long id);
}
