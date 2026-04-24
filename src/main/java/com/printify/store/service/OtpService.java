package com.printify.store.service;

import com.printify.store.dto.auth.SendOtpRequest;
import com.printify.store.dto.auth.VerifyOtpRequest;
import com.printify.store.entity.OtpVerification;
import com.printify.store.exception.BadRequestException;
import com.printify.store.repository.OtpVerificationRepository;
import com.printify.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final BrevoEmailService brevoEmailService;
    private final UserRepository userRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void sendOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String purpose = normalizePurpose(request.getPurpose());

        if ("REGISTER".equals(purpose) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));

        otpRepository.deleteAllByEmailAndPurpose(email, purpose);

        OtpVerification verification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .purpose(purpose)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpRepository.save(verification);
        brevoEmailService.sendOtpEmail(email, otp);
    }

    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String purpose = normalizePurpose(request.getPurpose());

        OtpVerification verification = otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new BadRequestException("OTP not found. Please request a new OTP."));

        if (verification.isVerified()) {
            return;
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired. Please request a new OTP.");
        }

        if (!verification.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        verification.setVerified(true);
        otpRepository.save(verification);
    }

    public boolean isEmailVerifiedForRegister(String email) {
        return otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email.trim().toLowerCase(), "REGISTER")
                .filter(OtpVerification::isVerified)
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "REGISTER";
        }
        return purpose.trim().toUpperCase();
    }
}