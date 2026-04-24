package com.printify.store.config;

import com.printify.store.entity.Role;
import com.printify.store.entity.User;
import com.printify.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-email:}")
    private String adminEmail;

    @Value("${app.admin.bootstrap-password:}")
    private String adminPassword;

    @Value("${app.admin.bootstrap-name:Admin}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User admin = User.builder()
                    .fullName(adminName)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            return userRepository.save(admin);
        });
    }
}