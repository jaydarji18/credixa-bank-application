package com.credixa.service;

import com.credixa.dto.request.LoginRequestDTO;
import com.credixa.dto.request.AdminRegisterRequestDTO;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.entity.AdminUser;
import com.credixa.exception.BadRequestException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnauthorizedException;
import com.credixa.repository.AdminUserRepository;
import com.credixa.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private AdminRegisterRequestDTO registerRequest;
    private AdminUser testAdmin;

    @BeforeEach
    void setUp() {
        registerRequest = AdminRegisterRequestDTO.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@credixa.com")
                .password("AdminPass@123")
                .role("SUPER_ADMIN")
                .build();

        testAdmin = AdminUser.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@credixa.com")
                .passwordHash("encodedPassword")
                .role(AdminUser.AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should login successfully for active admin")
    void shouldLoginSuccessfully() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@credixa.com");
        request.setPassword("AdminPass@123");

        given(adminUserRepository.findByEmail("admin@credixa.com"))
                .willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(tokenProvider.generateAccessToken(any(Authentication.class))).willReturn("accessToken");
        given(tokenProvider.generateRefreshToken(any(Authentication.class))).willReturn("refreshToken");

        AuthResponseDTO response = adminAuthService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("admin@credixa.com");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for non-existent admin")
    void shouldThrowExceptionForNonExistentAdmin() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("nonexistent@credixa.com");
        request.setPassword("password");

        given(adminUserRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid admin credentials");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for inactive admin")
    void shouldThrowExceptionForInactiveAdmin() {
        testAdmin.setActive(false);

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@credixa.com");
        request.setPassword("password");

        given(adminUserRepository.findByEmail("admin@credixa.com"))
                .willReturn(Optional.of(testAdmin));

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Admin account is inactive");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for wrong password")
    void shouldThrowExceptionForWrongPassword() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@credixa.com");
        request.setPassword("wrongpassword");

        given(adminUserRepository.findByEmail("admin@credixa.com"))
                .willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid admin credentials");
    }

    @Test
    @DisplayName("Should register admin successfully")
    void shouldRegisterAdmin() {
        given(adminUserRepository.findByEmail("admin@credixa.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(adminUserRepository.save(any(AdminUser.class))).willAnswer(invocation -> {
            AdminUser admin = invocation.getArgument(0);
            admin.setId(1L);
            return admin;
        });

        adminAuthService.register(registerRequest);

        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        given(adminUserRepository.findByEmail("admin@credixa.com"))
                .willReturn(Optional.of(testAdmin));

        assertThatThrownBy(() -> adminAuthService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Admin email already registered");
    }

    @Test
    @DisplayName("Should set admin as inactive by default")
    void shouldSetAdminInactiveByDefault() {
        given(adminUserRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(adminUserRepository.save(any(AdminUser.class))).willAnswer(invocation -> {
            AdminUser admin = invocation.getArgument(0);
            admin.setId(1L);
            return admin;
        });

        adminAuthService.register(registerRequest);

        verify(adminUserRepository).save(argThat(admin -> 
                admin.isActive() == false
        ));
    }
}