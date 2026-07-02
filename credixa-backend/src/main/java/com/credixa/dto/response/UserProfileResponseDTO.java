package com.credixa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDTO {
    private String userCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String aadhaarNumber;
    private String panNumber;
    private String profilePhotoUrl;
    private String status;
    private String kycStatus;
    private String twoFaMethod;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean twoFaEnabled;
    private boolean spinSet;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
