package com.credixa.service;

import com.credixa.config.JwtConfig;
import com.credixa.dto.request.*;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.dto.response.TwoFaResponseDTO;
import com.credixa.dto.response.UserSummaryDTO;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ForbiddenException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnauthorizedException;
import com.credixa.repository.UserRepository;
import com.credixa.security.JwtTokenProvider;
import com.credixa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;
    private final Map<String, Object> inMemoryCache = new ConcurrentHashMap<>();

    @Transactional
    public void register(RegisterRequestDTO request) {
        // Validate age
        if (Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            throw new BadRequestException("User must be at least 18 years old");
        }

        // Check uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Generate userCode (simplistic for now, should ideally use a sequence)
        String userCode = "USR" + String.format("%06d", userRepository.count() + 1);

        User user = User.builder()
                .userCode(userCode)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .dateOfBirth(request.getDateOfBirth())
                .aadhaarNumber(request.getAadhaarNumber())
                .panNumber(request.getPanNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .kycStatus(User.KycStatus.NOT_SUBMITTED)
                .build();

        userRepository.save(user);

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        // In-memory OTP storage
        inMemoryCache.put("OTP:EMAIL:" + user.getEmail(), otp);

        // Send real email via NotificationService
        notificationService.sendOtpEmail(user.getEmail(), otp, "Registration Verification");
        log.info("REGISTRATION OTP for {}: {}", user.getEmail(), otp);
    }

    @Transactional
    public AuthResponseDTO verifyOtp(OtpVerifyRequestDTO request) {
        String key = "OTP:" + request.getChannel() + ":" + request.getEmail();
        String cachedOtp = (String) inMemoryCache.get(key);

        if (cachedOtp == null) {
            throw new BadRequestException("OTP expired or invalid");
        }
        if (!cachedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("SMS".equals(request.getChannel())) {
            user.setPhoneVerified(true);
            log.info("Phone verified for user: {}", user.getEmail());
        } else {
            user.setEmailVerified(true);
            log.info("Email verified for user: {}", user.getEmail());
        }

        if (user.isEmailVerified() && user.isPhoneVerified()) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        
        userRepository.save(user);

        inMemoryCache.remove(key);

        return generateAuthResponse(user);
    }

    public void resendOtp(ResendOtpRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == User.UserStatus.BLOCKED || user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new ForbiddenException("Account is " + user.getStatus());
        }

        // Generate new OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        String key = "OTP:" + request.getChannel() + ":" + request.getEmail();
        inMemoryCache.put(key, otp);

        // Send real message via appropriate channel
        if ("EMAIL".equals(request.getChannel())) {
            notificationService.sendOtpEmail(user.getEmail(), otp, "OTP Verification");
        }
        log.info("RESEND OTP ({}) for {}: {}", request.getChannel(), user.getEmail(), otp);
    }

    public Object login(LoginRequestDTO request, String ipAddress) {
        // Simple Rate Limiting using in-memory map
        String rateLimitKey = "RATE_LIMIT:LOGIN:" + ipAddress;
        Long count = (Long) inMemoryCache.compute(rateLimitKey, (k, v) -> (v == null) ? 1L : ((Long) v) + 1);
        if (count != null && count > 10) {
            throw new com.credixa.exception.TooManyRequestsException("Too many login attempts. Please try again after 1 minute.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Account Lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ForbiddenException("Account locked. Try again after " + user.getLockedUntil());
        }

        if (user.getStatus() == User.UserStatus.BLOCKED || user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Account is " + user.getStatus());
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Account locked for user: {}", user.getEmail());
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid email or password");
        }

        /* 
        // Enforce Phone Verification check after successful password match
        if (!user.isPhoneVerified()) {
            String otp = String.format("%06d", new Random().nextInt(1000000));
            inMemoryCache.put("OTP:SMS:" + user.getEmail(), otp);
            notificationService.sendOtpSms(user.getPhone(), otp, "Phone Verification");
            
            String sessionToken = UUID.randomUUID().toString();
            inMemoryCache.put("PHONE_VERIFY_SESSION:" + sessionToken, user.getEmail());
            
            return TwoFaResponseDTO.builder()
                    .success(false)
                    .twoFaRequired(true)
                    .sessionToken(sessionToken)
                    .message("PHONE_VERIFICATION_REQUIRED")
                    .build();
        }
        */

        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            // If phone is verified (handled above), check email
            if (!user.isEmailVerified()) {
                throw new UnauthorizedException("Please verify your email first");
            }
            // If both verified but status still PENDING, update to ACTIVE
            user.setStatus(User.UserStatus.ACTIVE);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        if (user.isTwoFaEnabled()) {
            String sessionToken = UUID.randomUUID().toString();
            inMemoryCache.put("2FA_SESSION:" + sessionToken, user.getEmail());
            return TwoFaResponseDTO.builder()
                    .success(true)
                    .twoFaRequired(true)
                    .sessionToken(sessionToken)
                    .message("2FA verification required")
                    .build();
        }

        return generateAuthResponse(user);
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String userCode = tokenProvider.getUserCodeFromJWT(request.getRefreshToken());
        String cachedToken = (String) inMemoryCache.get("REFRESH:" + userCode);

        if (cachedToken == null || !cachedToken.equals(request.getRefreshToken())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return generateAuthResponse(user);
    }

    public void logout(LogoutRequestDTO request, String accessToken) {
        String userCode = tokenProvider.getUserCodeFromJWT(request.getRefreshToken());
        inMemoryCache.remove("REFRESH:" + userCode);

        // Blacklist access token
        inMemoryCache.put("BLACKLIST:" + accessToken, "true");
    }

    public void forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String rateLimitKey = "FORGOT_PWD_COUNT:" + request.getEmail();
        Integer count = (Integer) inMemoryCache.get(rateLimitKey);
        if (count != null && count >= 3) {
            throw new com.credixa.exception.TooManyRequestsException("Too many reset attempts. Please try again after 15 minutes.");
        }

        inMemoryCache.put(rateLimitKey, (count == null ? 0 : count) + 1);

        String otp = String.format("%06d", new Random().nextInt(1000000));
        inMemoryCache.put("OTP:EMAIL:" + request.getEmail(), otp);

        notificationService.sendOtpEmail(request.getEmail(), otp, "Password Reset");
        log.info("FORGOT PASSWORD OTP for {}: {}", request.getEmail(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        String key = "OTP:EMAIL:" + request.getEmail();
        String cachedOtp = (String) inMemoryCache.get(key);

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all sessions
        inMemoryCache.remove("REFRESH:" + user.getUserCode());
        inMemoryCache.remove(key);
    }

    private AuthResponseDTO generateAuthResponse(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create(user.getUserCode(), user.getEmail(), user.getPasswordHash(), "ROLE_USER"),
                null
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        inMemoryCache.put("REFRESH:" + user.getUserCode(), refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummaryDTO.builder()
                        .userCode(user.getUserCode())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .role("USER")
                        .kycStatus(user.getKycStatus().name())
                        .spinSet(user.getSpinHash() != null)
                        .build())
                .build();
    }
}
