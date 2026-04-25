package com.printify.store.service;

import com.printify.store.dto.payment.CreateRazorpayOrderResponse;
import com.printify.store.exception.BadRequestException;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.User;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final CartService cartService;

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

            BigDecimal total = cartItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int amountInPaise = total.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", "order_" + System.currentTimeMillis());
            options.put("payment_capture", 1);

            Order order = razorpay.orders.create(options);

            return CreateRazorpayOrderResponse.builder()
                    .razorpayOrderId(order.get("id"))
                    .currency("INR")
                    .amount(amountInPaise)
                    .displayAmount(total)
                    .keyId(keyId)
                    .build();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not create Razorpay order", e);
        }
    }

    public void verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret);

            if (!valid) {
                throw new BadRequestException("Payment verification failed");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Payment verification failed");
        }
    }
}