package com.printify.store.controller;

import com.printify.store.dto.payment.CreateRazorpayOrderRequest;
import com.printify.store.dto.payment.CreateRazorpayOrderResponse;
import com.printify.store.dto.payment.VerifyRazorpayPaymentRequest;
import com.printify.store.entity.CheckoutQuote;
import com.printify.store.entity.Order;
import com.printify.store.entity.User;
import com.printify.store.service.CurrentUserService;
import com.printify.store.service.OrderService;
import com.printify.store.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/razorpay")
@RequiredArgsConstructor
public class PaymentController {

    private final RazorpayPaymentService razorpayPaymentService;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @PostMapping("/order")
    public CreateRazorpayOrderResponse createRazorpayOrder(
            Authentication authentication,
            @Valid @RequestBody CreateRazorpayOrderRequest request
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        return razorpayPaymentService.createOrder(user, request.getCheckout());
    }

    @PostMapping("/verify")
    public Order verifyPaymentAndPlaceOrder(
            Authentication authentication,
            @Valid @RequestBody VerifyRazorpayPaymentRequest request
    ) {
        User user = currentUserService.getCurrentUser(authentication);

        CheckoutQuote quote = razorpayPaymentService.verifyPayment(
                user,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        return orderService.checkoutAfterVerifiedQuote(user, quote);
    }
}