package com.printify.store.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 80, message = "Name must be 2 to 80 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10 digit Indian mobile number")
        String mobile,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 1000, message = "Message must be 10 to 1000 characters")
        String message
) {}