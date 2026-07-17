package com.credixa.service;

import com.credixa.dto.request.UpdateProfileRequestDTO;
import com.credixa.dto.response.UserProfileResponseDTO;
import com.credixa.entity.User;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .passwordHash("encodedPassword")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .address("123 Main St")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .status(User.UserStatus.ACTIVE)
                .kycStatus(User.KycStatus.VERIFIED)
                .twoFaMethod(User.TwoFaMethod.NONE)
                .profilePhotoUrl(null)
                .spinHash(null)
                .build();
    }

    @Nested
    @DisplayName("getProfile() tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should return user profile successfully")
        void shouldReturnUserProfile() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));

            UserProfileResponseDTO response = userService.getProfile("USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getUserCode()).isEqualTo("USR00001");
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with code: USR99999");
        }
    }

    @Nested
    @DisplayName("updateProfile() tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should throw BadRequestException when updating non-existent user")
        void shouldThrowExceptionWhenUserNotFound() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setFirstName("Jane");
            request.setCity("Delhi");

            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile("USR99999", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("submitKyc() tests")
    class SubmitKycTests {

        @Test
        @DisplayName("Should throw BadRequestException when KYC already verified")
        void shouldThrowExceptionWhenKycAlreadyVerified() {
            testUser.setKycStatus(User.KycStatus.VERIFIED);

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.submitKyc("USR00001"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("KYC is already verified");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found for KYC submission")
        void shouldThrowExceptionWhenUserNotFoundForKyc() {
            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.submitKyc("USR99999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setSpin() and verifySpin() tests")
    class SpinTests {

        @Test
        @DisplayName("Should throw BadRequestException when sPin not set for user")
        void shouldThrowExceptionWhenSpinNotSet() {
            testUser.setSpinHash(null);

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.verifySpin("USR00001", "1234"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("sPin is not set for this user");
        }
    }
}