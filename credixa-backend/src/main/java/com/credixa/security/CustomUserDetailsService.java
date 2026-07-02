package com.credixa.security;

import com.credixa.entity.User;
import com.credixa.repository.AdminUserRepository;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to find in regular users first
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            return UserPrincipal.create(user.getUserCode(), user.getEmail(), user.getPasswordHash(), "ROLE_USER");
        }

        // Try to find in admin users
        var adminOpt = adminUserRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            var admin = adminOpt.get();
            return UserPrincipal.create("ADMIN_" + admin.getId(), admin.getEmail(), admin.getPasswordHash(), "ROLE_" + admin.getRole().name());
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
