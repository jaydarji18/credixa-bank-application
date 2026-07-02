package com.credixa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", nullable = false, unique = true)
    private String userCode;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "spin_hash")
    private String spinHash;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(name = "aadhaar_number", nullable = false, unique = true)
    private String aadhaarNumber;

    @com.credixa.validation.PanNumber
    @Column(name = "pan_number", nullable = false, unique = true)
    private String panNumber;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "two_fa_method", nullable = false)
    @Builder.Default
    private TwoFaMethod twoFaMethod = TwoFaMethod.NONE;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "two_fa_enabled", nullable = false)
    @Builder.Default
    private boolean twoFaEnabled = false;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "kyc_remarks")
    private String kycRemarks;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum UserStatus {
        PENDING_VERIFICATION, ACTIVE, BLOCKED, SUSPENDED, CLOSED
    }

    public enum KycStatus {
        NOT_SUBMITTED, PENDING, VERIFIED, REJECTED
    }

    public enum TwoFaMethod {
        NONE, SMS_OTP, AUTHENTICATOR_APP, BOTH
    }
}
