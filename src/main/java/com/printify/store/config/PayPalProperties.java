package com.printify.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "paypal")
public class PayPalProperties {
    private String mode;
    private String clientId;
    private String clientSecret;
    private String frontendUrl;

    public String baseUrl() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }
}