package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_verifications")
public class OtpVerification extends BaseDocument {
    private String email;
    private String otp;
    private String purpose;
    private boolean verified;
    private LocalDateTime expiresAt;
}