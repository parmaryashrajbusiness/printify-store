package com.printify.store.controller;

import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.dto.payment.PayPalCreateOrderResponse;
import com.printify.store.service.PayPalPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/paypal")
public class PayPalPaymentController {

    private final PayPalPaymentService payPalPaymentService;

    @PostMapping("/create-order")
    public PayPalCreateOrderResponse createOrder(
            Authentication authentication,
            @RequestBody CheckoutRequest request
    ) {
        return payPalPaymentService.createPayPalOrder(authentication.getName(), request);
    }

    @PostMapping("/capture")
    public Object capture(
            Authentication authentication,
            @RequestBody Map<String, Object> body
    ) {
        String paypalOrderId = String.valueOf(body.get("paypalOrderId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> shippingMap = (Map<String, Object>) body.get("shipping");

        CheckoutRequest request = new CheckoutRequest();
        request.setFullName(String.valueOf(shippingMap.get("fullName")));
        request.setEmail(String.valueOf(shippingMap.get("email")));
        request.setPhone(String.valueOf(shippingMap.get("phone")));
        request.setAddressLine1(String.valueOf(shippingMap.get("addressLine1")));
        request.setAddressLine2(String.valueOf(shippingMap.get("addressLine2")));
        request.setCity(String.valueOf(shippingMap.get("city")));
        request.setState(String.valueOf(shippingMap.get("state")));
        request.setPostalCode(String.valueOf(shippingMap.get("postalCode")));
        request.setCountry(String.valueOf(shippingMap.get("country")));

        return payPalPaymentService.capturePayPalOrder(
                authentication.getName(),
                paypalOrderId,
                request
        );
    }
}