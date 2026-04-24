package com.printify.store.controller;

import com.printify.store.dto.auth.*;
import com.printify.store.entity.User;
import com.printify.store.repository.UserRepository;
import com.printify.store.service.AuthService;
import com.printify.store.service.CurrentUserService;
import com.printify.store.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final CurrentUserService currentUserService;

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request);
        return Map.of(
                "success", true,
                "message", "OTP sent successfully"
        );
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request);
        return Map.of(
                "success", true,
                "message", "OTP verified successfully"
        );
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }
}