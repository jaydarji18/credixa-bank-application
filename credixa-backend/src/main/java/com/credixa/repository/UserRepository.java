package com.credixa.repository;

import com.credixa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    long countByCreatedAtAfter(LocalDateTime date);
    long countByStatusIn(Collection<User.UserStatus> statuses);
    long countByKycStatus(User.KycStatus kycStatus);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.userCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "u.phone LIKE CONCAT('%', :search, '%')) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:kycStatus IS NULL OR u.kycStatus = :kycStatus)")
    Page<User> findAllAdmin(@Param("search") String search, 
                           @Param("status") User.UserStatus status, 
                           @Param("kycStatus") User.KycStatus kycStatus, 
                           Pageable pageable);
}
