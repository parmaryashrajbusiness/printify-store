package com.printify.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrevoEmailService {

    private final WebClient webClient;

    @Value("${app.brevo.api-url}")
    private String apiUrl;

    @Value("${app.brevo.api-key}")
    private String apiKey;

    @Value("${app.brevo.sender-name}")
    private String senderName;

    @Value("${app.brevo.sender-email}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of(
                                "email", toEmail
                        )
                ),
                "subject", "Your NeonCart verification code",
                "htmlContent", buildOtpHtml(otp)
        );

        webClient.post()
                .uri(apiUrl)
                .header("api-key", apiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String buildOtpHtml(String otp) {
        return """
                <div style="font-family:Arial,sans-serif;background:#050805;padding:32px;color:#ffffff;">
                  <div style="max-width:520px;margin:auto;background:#0b120c;border:1px solid #1f3d28;border-radius:18px;padding:28px;">
                    <h2 style="margin:0 0 12px;color:#22c55e;">Verify your email</h2>
                    <p style="color:#d4d4d8;font-size:15px;line-height:1.6;">
                      Use the OTP below to complete your NeonCart registration.
                    </p>
                    <div style="margin:24px 0;padding:18px;border-radius:14px;background:#111827;text-align:center;">
                      <span style="font-size:34px;letter-spacing:8px;font-weight:bold;color:#22c55e;">%s</span>
                    </div>
                    <p style="color:#a1a1aa;font-size:13px;">
                      This code will expire in 10 minutes. If you did not request this, you can ignore this email.
                    </p>
                  </div>
                </div>
                """.formatted(otp);
    }
}