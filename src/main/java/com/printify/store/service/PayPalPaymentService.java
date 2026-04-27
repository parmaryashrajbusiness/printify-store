package com.printify.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.printify.store.config.PayPalProperties;
import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.dto.payment.PayPalCreateOrderResponse;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.User;
import com.printify.store.exception.BadRequestException;
import com.printify.store.repository.CartItemRepository;
import com.printify.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayPalPaymentService {

    private final PayPalProperties properties;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.currency.inr-to-usd-rate:83.00}")
    private BigDecimal inrToUsdRate;

    public PayPalCreateOrderResponse createPayPalOrder(String userId, CheckoutRequest request) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty.");
        }

        if ("IN".equalsIgnoreCase(request.getCountry())) {
            throw new BadRequestException("PayPal is not available for Indian checkout. Please use Razorpay.");
        }

        BigDecimal totalInr = calculateCartTotalInr(cartItems);
        BigDecimal totalUsd = convertInrToUsd(totalInr);

        if (totalUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid PayPal total.");
        }

        String token = getAccessToken();

        String returnUrl = properties.getFrontendUrl() + "/paypal/success";
        String cancelUrl = properties.getFrontendUrl() + "/paypal/cancel";

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of(
                                "currency_code", "USD",
                                "value", moneyValue(totalUsd)
                        ),
                        "description", "NeonCart order"
                )),
                "application_context", Map.of(
                        "brand_name", "NeonCart",
                        "landing_page", "LOGIN",
                        "user_action", "PAY_NOW",
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl
                )
        );

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                properties.baseUrl() + "/v2/checkout/orders",
                HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)),
                JsonNode.class
        );

        JsonNode json = response.getBody();

        if (json == null || json.get("id") == null) {
            throw new BadRequestException("Could not create PayPal order.");
        }

        String paypalOrderId = json.get("id").asText();
        String approvalUrl = null;

        for (JsonNode link : json.get("links")) {
            if ("approve".equalsIgnoreCase(link.get("rel").asText())) {
                approvalUrl = link.get("href").asText();
                break;
            }
        }

        if (approvalUrl == null) {
            throw new BadRequestException("PayPal approval URL not found.");
        }

        return PayPalCreateOrderResponse.builder()
                .paypalOrderId(paypalOrderId)
                .approvalUrl(approvalUrl)
                .build();
    }

    public Object capturePayPalOrder(String userId, String paypalOrderId, CheckoutRequest checkoutRequest) {
        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            throw new BadRequestException("PayPal order id is required.");
        }

        if ("IN".equalsIgnoreCase(checkoutRequest.getCountry())) {
            throw new BadRequestException("PayPal is not available for Indian checkout. Please use Razorpay.");
        }

        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty.");
        }

        String token = getAccessToken();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                properties.baseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class
        );

        JsonNode json = response.getBody();

        if (json == null || !"COMPLETED".equalsIgnoreCase(json.path("status").asText())) {
            throw new BadRequestException("PayPal payment was not completed.");
        }

        String captureId = json
                .path("purchase_units")
                .path(0)
                .path("payments")
                .path("captures")
                .path(0)
                .path("id")
                .asText();

        if (captureId == null || captureId.isBlank()) {
            throw new BadRequestException("PayPal capture id not found.");
        }

        String paidCurrency = extractPaidCurrency(json);
        BigDecimal paidAmount = extractPaidAmount(json);

        if (!"USD".equalsIgnoreCase(paidCurrency)) {
            throw new BadRequestException("Invalid PayPal payment currency.");
        }

        BigDecimal expectedInr = calculateCartTotalInr(cartItems);
        BigDecimal expectedUsd = convertInrToUsd(expectedInr);

        if (paidAmount.compareTo(expectedUsd) != 0) {
            throw new BadRequestException("Payment amount mismatch.");
        }

        return orderService.checkoutAfterOnlinePayment(
                user,
                checkoutRequest,
                "PAYPAL",
                paypalOrderId,
                captureId,
                "USD",
                paidAmount
        );
    }

    private BigDecimal calculateCartTotalInr(List<CartItem> cartItems) {
        BigDecimal total = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid cart total.");
        }

        return total;
    }

    private BigDecimal convertInrToUsd(BigDecimal inrAmount) {
        if (inrToUsdRate == null || inrToUsdRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid INR to USD conversion rate.");
        }

        return inrAmount.divide(inrToUsdRate, 2, RoundingMode.HALF_UP);
    }

    private String moneyValue(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal extractPaidAmount(JsonNode json) {
        try {
            String value = json
                    .path("purchase_units")
                    .path(0)
                    .path("payments")
                    .path("captures")
                    .path(0)
                    .path("amount")
                    .path("value")
                    .asText();

            if (value == null || value.isBlank()) {
                throw new BadRequestException("PayPal paid amount not found.");
            }

            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new BadRequestException("Failed to extract PayPal amount.");
        }
    }

    private String extractPaidCurrency(JsonNode json) {
        String currency = json
                .path("purchase_units")
                .path(0)
                .path("payments")
                .path("captures")
                .path(0)
                .path("amount")
                .path("currency_code")
                .asText();

        if (currency == null || currency.isBlank()) {
            throw new BadRequestException("PayPal paid currency not found.");
        }

        return currency;
    }

    private String getAccessToken() {
        Assert.hasText(properties.getClientId(), "PAYPAL_CLIENT_ID is missing");
        Assert.hasText(properties.getClientSecret(), "PAYPAL_CLIENT_SECRET is missing");

        String auth = properties.getClientId() + ":" + properties.getClientSecret();
        Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                properties.baseUrl() + "/v1/oauth2/token",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                JsonNode.class
        );

        JsonNode json = response.getBody();

        if (json == null || json.get("access_token") == null) {
            throw new BadRequestException("Could not get PayPal access token.");
        }

        return json.get("access_token").asText();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}