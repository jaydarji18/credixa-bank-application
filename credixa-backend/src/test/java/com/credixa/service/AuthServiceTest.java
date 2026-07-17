package com.credixa.service;

import com.credixa.dto.request.OtpVerifyRequestDTO;
import com.credixa.dto.request.ResendOtpRequestDTO;
import com.credixa.dto.request.LoginRequestDTO;
import com.credixa.dto.request.RefreshTokenRequestDTO;
import com.credixa.dto.request.LogoutRequestDTO;
import com.credixa.dto.request.ForgotPasswordRequestDTO;
import com.credixa.dto.request.ResetPasswordRequestDTO;
import com.credixa.dto.request.RegisterRequestDTO;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.dto.response.TwoFaResponseDTO;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ForbiddenException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnauthorizedException;
import com.credixa.exception.TooManyRequestsException;
import com.credixa.repository.UserRepository;
import com.credixa.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequestDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .password("Password@123")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .address("123 Main St")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .build();

        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .passwordHash("encodedPassword")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .status(User.UserStatus.PENDING_VERIFICATION)
                .kycStatus(User.KycStatus.NOT_SUBMITTED)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        clearRateLimitCache();
    }

    private void clearRateLimitCache() {
        try {
            Field field = AuthService.class.getDeclaredField("inMemoryCache");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = (Map<String, Object>) field.get(authService);
            cache.clear();
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("register() tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully when all validations pass")
        void shouldRegisterUserSuccessfully() {
            given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(false);
            given(userRepository.existsByPhone(registerRequest.getPhone())).willReturn(false);
            given(userRepository.count()).willReturn(0L);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            authService.register(registerRequest);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserCode()).startsWith("USR");
            assertThat(savedUser.getStatus()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
            assertThat(savedUser.getKycStatus()).isEqualTo(User.KycStatus.NOT_SUBMITTED);
            verify(notificationService).sendOtpEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when user is under 18 years old")
        void shouldThrowExceptionWhenUserUnder18() {
            RegisterRequestDTO request = RegisterRequestDTO.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("young@example.com")
                    .phone("9876543210")
                    .password("Password@123")
                    .dateOfBirth(LocalDate.now().minusYears(17))
                    .panNumber("ABCDE1234F")
                    .aadhaarNumber("123456789012")
                    .address("123 Main St")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400001")
                    .build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("at least 18 years old");
        }

        @Test
        @DisplayName("Should throw BadRequestException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email already registered");
        }

        @Test
        @DisplayName("Should throw BadRequestException when phone already exists")
        void shouldThrowExceptionWhenPhoneExists() {
            given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(false);
            given(userRepository.existsByPhone(registerRequest.getPhone())).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Phone number already registered");
        }
    }

    @Nested
    @DisplayName("verifyOtp() tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should throw BadRequestException when user not found for verifyOtp")
        void shouldThrowExceptionWhenUserNotFound() {
            OtpVerifyRequestDTO request = new OtpVerifyRequestDTO();
            request.setEmail("nonexistent@example.com");
            request.setOtp("123456");
            request.setChannel(OtpVerifyRequestDTO.OtpChannel.EMAIL);

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("OTP expired or invalid");
        }
    }

    @Nested
    @DisplayName("resendOtp() tests")
    class ResendOtpTests {

        @Test
        @DisplayName("Should throw ForbiddenException when account is BLOCKED")
        void shouldThrowExceptionWhenAccountBlocked() {
            testUser.setStatus(User.UserStatus.BLOCKED);
            
            ResendOtpRequestDTO request = new ResendOtpRequestDTO();
            request.setEmail(testUser.getEmail());
            request.setChannel(OtpVerifyRequestDTO.OtpChannel.EMAIL);

            given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.resendOtp(request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Account is BLOCKED");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found for resend")
        void shouldThrowExceptionWhenUserNotFoundForResend() {
            ResendOtpRequestDTO request = new ResendOtpRequestDTO();
            request.setEmail("nonexistent@example.com");
            request.setChannel(OtpVerifyRequestDTO.OtpChannel.EMAIL);

            given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("login() tests")
    class LoginTests {

        @Test
        @DisplayName("Should throw TooManyRequestsException after rate limit")
        void shouldThrowExceptionAfterRateLimit() {
            clearRateLimitCache();
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("test@example.com");
            request.setPassword("wrongpassword");

            // Return fresh user each time to avoid state pollution
            given(userRepository.findByEmail(anyString())).willAnswer(invocation -> Optional.of(
                User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("encodedPassword")
                    .status(User.UserStatus.ACTIVE)
                    .lockedUntil(null)
                    .failedLoginAttempts(0)
                    .build()
            ));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // Simulate 10 previous attempts
            for (int i = 0; i < 10; i++) {
                try {
                    authService.login(request, "10.0.0.25");
                } catch (UnauthorizedException e) {
                    // Expected for wrong password
                }
            }

            // 11th attempt should trigger rate limit
            assertThatThrownBy(() -> authService.login(request, "10.0.0.25"))
                    .isInstanceOf(TooManyRequestsException.class)
                    .hasMessageContaining("Too many login attempts");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user not found for login")
        void shouldReturnUnauthorizedWhenUserNotFound() {
            clearRateLimitCache();
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("nonexistent@example.com");
            request.setPassword("wrongpassword");

            // UserRepository not mocked, will return Optional.empty
            assertThatThrownBy(() -> authService.login(request, "192.168.1.2"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}