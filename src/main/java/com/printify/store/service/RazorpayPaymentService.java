package com.printify.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.dto.payment.CreateRazorpayOrderResponse;
import com.printify.store.entity.*;
import com.printify.store.exception.BadRequestException;
import com.printify.store.repository.CheckoutQuoteRepository;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final CartService cartService;
    private final ProductService productService;
    private final PricingService pricingService;
    private final CheckoutQuoteRepository checkoutQuoteRepository;
    private final ObjectMapper objectMapper;

    @Value("${RAZORPAY_KEY_ID}")
    private String keyId;

    @Value("${RAZORPAY_KEY_SECRET}")
    private String keySecret;

    public CreateRazorpayOrderResponse createOrder(User user, CheckoutRequest checkout) {
        try {
            String country = pricingService.normalizeCountry(checkout.getCountry());
            String currency = pricingService.currencyForCountry(country);

            List<CartItem> cartItems = cartService.getCartItems(user);

            if (cartItems.isEmpty()) {
                throw new BadRequestException("Cart is empty");
            }

            List<OrderItem> quoteItems = cartItems.stream()
                    .map(cartItem -> buildQuoteItem(cartItem, currency))
                    .toList();

            BigDecimal subtotal = quoteItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grandTotal = pricingService.addFeeBuffer(subtotal, country);
            BigDecimal feeBuffer = grandTotal.subtract(subtotal);
            int amountMinorUnit = pricingService.toMinorUnit(grandTotal, currency);

            if (amountMinorUnit < 100) {
                throw new BadRequestException("Invalid payment amount");
            }

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", amountMinorUnit);
            options.put("currency", currency);
            options.put("receipt", "nc_" + System.currentTimeMillis());
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");

            CheckoutQuote quote = CheckoutQuote.builder()
                    .userId(user.getId())
                    .razorpayOrderId(razorpayOrderId)
                    .status("CREATED")
                    .shippingCountry(country)
                    .paymentCurrency(currency)
                    .subtotal(subtotal)
                    .feeBufferAmount(feeBuffer)
                    .grandTotal(grandTotal)
                    .amountMinorUnit(amountMinorUnit)
                    .items(quoteItems)
                    .checkoutSnapshotJson(objectMapper.writeValueAsString(checkout))
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            checkoutQuoteRepository.save(quote);

            return CreateRazorpayOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .currency(currency)
                    .amount(amountMinorUnit)
                    .displayAmount(grandTotal)
                    .keyId(keyId)
                    .build();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not create Razorpay order: " + e.getMessage(), e);
        }
    }

    public CheckoutQuote verifyPayment(
            User user,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
        try {
            CheckoutQuote quote = checkoutQuoteRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new BadRequestException("Payment quote not found"));

            if (!quote.getUserId().equals(user.getId())) {
                throw new BadRequestException("Payment quote does not belong to this user");
            }

            if (!"CREATED".equals(quote.getStatus())) {
                throw new BadRequestException("This payment quote is already used or expired");
            }

            if (quote.getExpiresAt() == null || quote.getExpiresAt().isBefore(LocalDateTime.now())) {
                quote.setStatus("EXPIRED");
                checkoutQuoteRepository.save(quote);
                throw new BadRequestException("Payment quote expired. Please checkout again.");
            }

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean validSignature = Utils.verifyPaymentSignature(attributes, keySecret);

            if (!validSignature) {
                throw new BadRequestException("Payment signature verification failed");
            }

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            Payment payment = razorpay.payments.fetch(razorpayPaymentId);

            String paymentOrderId = payment.get("order_id");
            String status = payment.get("status");
            Integer paidAmount = payment.get("amount");
            String currency = payment.get("currency");

            if (!razorpayOrderId.equals(paymentOrderId)) {
                throw new BadRequestException("Payment order mismatch");
            }

            if (!quote.getPaymentCurrency().equalsIgnoreCase(currency)) {
                throw new BadRequestException("Payment currency mismatch");
            }

            if (!quote.getAmountMinorUnit().equals(paidAmount)) {
                throw new BadRequestException("Payment amount mismatch");
            }

            if (!"captured".equalsIgnoreCase(status)) {
                throw new BadRequestException("Payment not captured");
            }

            quote.setStatus("PAID");
            quote.setRazorpayPaymentId(razorpayPaymentId);
            quote.setPaidAt(LocalDateTime.now());

            return checkoutQuoteRepository.save(quote);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Payment verification failed");
        }
    }

    private OrderItem buildQuoteItem(CartItem cartItem, String paymentCurrency) {
        Product product = productService.getById(cartItem.getProductId());

        ProductVariant variant = pricingService.resolveVariant(product, cartItem.getPrintifyVariantId());

        String printifyVariantId = variant != null
                ? variant.getPrintifyVariantId()
                : product.getDefaultVariantId();

        if (printifyVariantId == null || printifyVariantId.isBlank()) {
            throw new BadRequestException("Product variant is missing for " + product.getName());
        }

        BigDecimal originalPrice = pricingService.getOriginalUnitPrice(product, variant);
        String originalCurrency = pricingService.getOriginalCurrency(product, variant);
        BigDecimal convertedPrice = pricingService.convert(originalPrice, originalCurrency, paymentCurrency);

        return OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .imageUrl(product.getImageUrl())
                .colorway(cartItem.getVariantTitle() != null ? cartItem.getVariantTitle() : product.getColorway())
                .quantity(cartItem.getQuantity())
                .unitPrice(convertedPrice)
                .printifyProductId(product.getPrintifyProductId())
                .printifyVariantId(printifyVariantId)
                .build();
    }
}