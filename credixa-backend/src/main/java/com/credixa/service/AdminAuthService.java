package com.credixa.service;

import com.credixa.dto.request.LoginRequestDTO;
import com.credixa.dto.response.ApiResponse;
import com.credixa.dto.response.AuthResponseDTO;
import com.credixa.dto.response.UserSummaryDTO;
import com.credixa.entity.AdminUser;
import com.credixa.exception.UnauthorizedException;
import com.credixa.repository.AdminUserRepository;
import com.credixa.security.JwtTokenProvider;
import com.credixa.dto.request.AdminRegisterRequestDTO;
import com.credixa.exception.BadRequestException;
import com.credixa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final Map<String, Object> inMemoryCache = new ConcurrentHashMap<>();

    public AuthResponseDTO login(LoginRequestDTO request) {
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid admin credentials"));

        if (!admin.isActive()) {
            throw new UnauthorizedException("Admin account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid admin credentials");
        }

        return generateAdminAuthResponse(admin);
    }

    public void register(AdminRegisterRequestDTO request) {
        if (adminUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Admin email already registered");
        }

        AdminUser newAdmin = AdminUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AdminUser.AdminRole.valueOf(request.getRole()))
                .isActive(false) // Needs approval from SUPER_ADMIN
                .build();
        
        adminUserRepository.save(newAdmin);
        log.info("New admin application submitted by: {}", request.getEmail());
    }

    private AuthResponseDTO generateAdminAuthResponse(AdminUser admin) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create("ADMIN_" + admin.getId(), admin.getEmail(), admin.getPasswordHash(), "ROLE_" + admin.getRole().name()),
                null
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        inMemoryCache.put("REFRESH:ADMIN_" + admin.getId(), refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummaryDTO.builder()
                        .userCode("ADMIN_" + admin.getId())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .email(admin.getEmail())
                        .role(admin.getRole().name())
                        .build())
                .build();
    }
}
