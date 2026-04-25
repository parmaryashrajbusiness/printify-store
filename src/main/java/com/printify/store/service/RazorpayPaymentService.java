package com.printify.store.service;

import com.printify.store.dto.payment.CreateRazorpayOrderResponse;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.User;
import com.printify.store.exception.BadRequestException;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final CartService cartService;
    private final PricingService pricingService;

    @Value("${RAZORPAY_KEY_ID}")
    private String keyId;

    @Value("${RAZORPAY_KEY_SECRET}")
    private String keySecret;

    public CreateRazorpayOrderResponse createOrder(User user) {
        try {
            List<CartItem> cartItems = cartService.getCartItems(user);

            if (cartItems.isEmpty()) {
                throw new BadRequestException("Cart is empty");
            }

            BigDecimal totalInr = calculateCartTotalInr(cartItems);
            int amountInPaise = pricingService.toPaise(totalInr);

            if (amountInPaise < 100) {
                throw new BadRequestException("Invalid payment amount");
            }

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            String receipt = "nc_" + System.currentTimeMillis();
            options.put("receipt", receipt);
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(options);

            return CreateRazorpayOrderResponse.builder()
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .currency("INR")
                    .amount(amountInPaise)
                    .displayAmount(totalInr)
                    .keyId(keyId)
                    .build();

        } catch (BadRequestException e) {
            throw e;
        }  catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create Razorpay order: " + e.getMessage(), e);
        }
    }

    public void verifyPayment(
            User user,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean validSignature = Utils.verifyPaymentSignature(attributes, keySecret);

            if (!validSignature) {
                throw new BadRequestException("Payment verification failed");
            }

            List<CartItem> cartItems = cartService.getCartItems(user);
            BigDecimal expectedTotalInr = calculateCartTotalInr(cartItems);
            int expectedAmountPaise = pricingService.toPaise(expectedTotalInr);

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            Payment payment = razorpay.payments.fetch(razorpayPaymentId);

            String paymentOrderId = payment.get("order_id");
            String status = payment.get("status");
            Integer paidAmount = payment.get("amount");
            String currency = payment.get("currency");

            if (!razorpayOrderId.equals(paymentOrderId)) {
                throw new BadRequestException("Payment order mismatch");
            }

            if (!"INR".equalsIgnoreCase(currency)) {
                throw new BadRequestException("Invalid payment currency");
            }

            if (!Integer.valueOf(expectedAmountPaise).equals(paidAmount)) {
                throw new BadRequestException("Payment amount mismatch");
            }

            if (!"captured".equalsIgnoreCase(status)) {
                throw new BadRequestException("Payment is not captured yet");
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Payment verification failed");
        }
    }

    private BigDecimal calculateCartTotalInr(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}