package com.printify.store.service;

import com.printify.store.dto.contact.ContactRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final WebClient.Builder webClientBuilder;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME:NeonCart}")
    private String senderName;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    public void sendContactMessage(ContactRequest request) {
        sendAdminEmail(request);
        sendUserConfirmationEmail(request);
    }

    private void sendAdminEmail(ContactRequest request) {
        String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.6">
                    <h2>New Contact Message</h2>
                    <p><b>Name:</b> %s</p>
                    <p><b>Email:</b> %s</p>
                    <p><b>Mobile:</b> %s</p>
                    <p><b>Message:</b></p>
                    <div style="padding:12px;border-left:4px solid #22c55e;background:#f6f6f6">
                        %s
                    </div>
                    <br/>
                    <a href="tel:+91%s"
                       style="background:#22c55e;color:#000;padding:12px 18px;text-decoration:none;border-radius:8px;font-weight:bold">
                       Call Customer
                    </a>
                    <p style="margin-top:20px;color:#555">
                        You can reply directly to this email. The reply will go to the customer.
                    </p>
                </div>
                """.formatted(
                escapeHtml(request.name()),
                escapeHtml(request.email()),
                escapeHtml(request.mobile()),
                escapeHtml(request.message()).replace("\n", "<br/>"),
                escapeHtml(request.mobile())
        );

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(Map.of(
                        "email", adminEmail,
                        "name", "Admin"
                )),
                "replyTo", Map.of(
                        "email", request.email(),
                        "name", request.name()
                ),
                "subject", "New Contact Message from " + request.name(),
                "htmlContent", html
        );

        sendBrevoEmail(body);
    }

    private void sendUserConfirmationEmail(ContactRequest request) {
        String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.6">
                    <h2>Thanks for contacting NeonCart</h2>
                    <p>Hello %s,</p>
                    <p>We received your message and will contact you soon.</p>
                    <p><b>Your message:</b></p>
                    <div style="padding:12px;border-left:4px solid #22c55e;background:#f6f6f6">
                        %s
                    </div>
                    <p style="margin-top:20px;color:#555">
                        You can reply to this email if you want to add more details.
                    </p>
                </div>
                """.formatted(
                escapeHtml(request.name()),
                escapeHtml(request.message()).replace("\n", "<br/>")
        );

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(Map.of(
                        "email", request.email(),
                        "name", request.name()
                )),
                "replyTo", Map.of(
                        "email", adminEmail,
                        "name", senderName
                ),
                "subject", "We received your message",
                "htmlContent", html
        );

        sendBrevoEmail(body);
    }

    private void sendBrevoEmail(Map<String, Object> body) {
        webClientBuilder.build()
                .post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .header("api-key", brevoApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}