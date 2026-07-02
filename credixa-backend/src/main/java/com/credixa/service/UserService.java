package com.credixa.service;

import com.credixa.dto.request.ChangePasswordRequestDTO;
import com.credixa.dto.request.UpdateProfileRequestDTO;
import com.credixa.dto.response.UserProfileResponseDTO;
import com.credixa.entity.Notification;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final Map<String, Object> inMemoryCache = new ConcurrentHashMap<>();

    public UserProfileResponseDTO getProfile(String userCode) {
        User user = findUserByCode(userCode);
        return mapToProfileDTO(user);
    }

    @Transactional
    public UserProfileResponseDTO updateProfile(String userCode, UpdateProfileRequestDTO request) {
        User user = findUserByCode(userCode);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPincode() != null) user.setPincode(request.getPincode());

        return mapToProfileDTO(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String userCode, ChangePasswordRequestDTO request) {
        User user = findUserByCode(userCode);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions
        inMemoryCache.remove("REFRESH:" + userCode);
    }

    @Transactional
    public String uploadProfilePhoto(String userCode, MultipartFile file) {
        User user = findUserByCode(userCode);

        // Validate file
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 5MB limit");
        }

        try {
            // Path setup: using project root/uploads
            String uploadDir = "uploads/profile-photos";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = userCode + ".jpg";
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            String photoUrl = "/api/v1/users/photo/" + filename; // Assuming a static resource handler or controller
            user.setProfilePhotoUrl(photoUrl);
            userRepository.save(user);

            return photoUrl;
        } catch (IOException e) {
            log.error("Failed to upload profile photo", e);
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
    @Transactional
    public void submitKyc(String userCode) {
        User user = findUserByCode(userCode);
        if (user.getKycStatus() == User.KycStatus.VERIFIED) {
            throw new BadRequestException("KYC is already verified");
        }
        user.setKycStatus(User.KycStatus.PENDING);
        userRepository.save(user);
        log.info("KYC submitted for user: {}", userCode);
        
        notificationService.sendGlobalNotification(user, 
                "KYC Submitted", 
                "Your KYC application has been received and is currently under review.", 
                Notification.NotificationType.KYC_SUBMITTED);
    }

    @Transactional
    public void simulateKycVerification(String userCode) {
        User user = findUserByCode(userCode);
        user.setKycStatus(User.KycStatus.VERIFIED);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("KYC simulations: User {} is now VERIFIED and ACTIVE", userCode);
        
        notificationService.sendGlobalNotification(user, 
                "KYC Approved", 
                "Congratulations! Your KYC verification is complete. Your account is now fully active.", 
                Notification.NotificationType.KYC_APPROVED);
    }

    @Transactional
    public void setSpin(String userCode, String spin) {
        User user = findUserByCode(userCode);
        user.setSpinHash(passwordEncoder.encode(spin));
        userRepository.save(user);
        log.info("sPin set successfully for user: {}", userCode);
    }

    public void verifySpin(String userCode, String spin) {
        User user = findUserByCode(userCode);
        if (user.getSpinHash() == null) {
            throw new BadRequestException("sPin is not set for this user");
        }
        if (!passwordEncoder.matches(spin, user.getSpinHash())) {
            throw new BadRequestException("Invalid sPin provided");
        }
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with code: " + userCode));
    }

    private UserProfileResponseDTO mapToProfileDTO(User user) {
        return UserProfileResponseDTO.builder()
                .userCode(user.getUserCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .pincode(user.getPincode())
                .aadhaarNumber(user.getAadhaarNumber())
                .panNumber(user.getPanNumber())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .status(user.getStatus().name())
                .kycStatus(user.getKycStatus().name())
                .twoFaMethod(user.getTwoFaMethod().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .twoFaEnabled(user.isTwoFaEnabled())
                .spinSet(user.getSpinHash() != null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
