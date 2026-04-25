package com.printify.store.service;

import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.dto.order.OrderTrackingResponse;
import com.printify.store.dto.printify.PrintifyOrderSnapshot;
import com.printify.store.entity.*;
import com.printify.store.exception.BadRequestException;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PrintifyService printifyService;
    private final ShippingValidationService shippingValidationService;

    public List<Order> getOrders(User user) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Order checkout(User user, CheckoutRequest request) {
        shippingValidationService.validate(request);

        List<CartItem> cartItems = cartService.getCartItems(user);

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<OrderItem> items = cartItems.stream()
                .map(cartItem -> {
                    Product product = productService.getById(cartItem.getProductId());

                    String printifyVariantId = cartItem.getPrintifyVariantId();

                    if (printifyVariantId == null || printifyVariantId.isBlank()) {
                        printifyVariantId = product.getDefaultVariantId();
                    }

                    if (printifyVariantId == null || printifyVariantId.isBlank()) {
                        throw new BadRequestException("Product variant is missing for " + product.getName());
                    }

                    return OrderItem.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .productSlug(product.getSlug())
                            .imageUrl(product.getImageUrl())
                            .colorway(cartItem.getVariantTitle() != null
                                    ? cartItem.getVariantTitle()
                                    : product.getColorway())
                            .quantity(cartItem.getQuantity())
                            .unitPrice(cartItem.getUnitPrice())
                            .printifyProductId(product.getPrintifyProductId())
                            .printifyVariantId(printifyVariantId)
                            .build();
                })
                .toList();

        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(user.getId())
                .status("PENDING")
                .totalAmount(total)
                .shippingFullName(request.getFullName())
                .shippingEmail(request.getEmail())
                .shippingPhone(request.getPhone())
                .shippingAddressLine1(request.getAddressLine1())
                .shippingAddressLine2(request.getAddressLine2())
                .shippingCity(request.getCity())
                .shippingState(request.getState())
                .shippingPostalCode(request.getPostalCode())
                .shippingCountry(request.getCountry())
                .items(items)
                .build();

        order = orderRepository.save(order);

        PrintifyOrderSnapshot snapshot = printifyService.createOrder(order);

        applyPrintifySnapshot(order, snapshot);
        order.setStatus(toLocalStatus(snapshot.getStatus()));

        order = orderRepository.save(order);

        cartService.clearCart(user);

        return order;
    }

    public OrderTrackingResponse getTracking(User user, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPrintifyOrderId() != null && !order.getPrintifyOrderId().isBlank()) {
            try {
                PrintifyOrderSnapshot snapshot = printifyService.getOrder(order.getPrintifyOrderId());

                applyPrintifySnapshot(order, snapshot);
                order.setStatus(toLocalStatus(snapshot.getStatus()));

                order = orderRepository.save(order);
            } catch (Exception ignored) {
                // If Printify is temporarily unavailable, keep the last saved DB status.
            }
        }

        return toTrackingResponse(order);
    }

    private void applyPrintifySnapshot(Order order, PrintifyOrderSnapshot snapshot) {
        order.setPrintifyOrderId(snapshot.getPrintifyOrderId());
        order.setPrintifyAppOrderId(snapshot.getAppOrderId());
        order.setPrintifyShopId(snapshot.getShopId());
        order.setPrintifyStatus(snapshot.getStatus());
        order.setPrintifyConnectUrl(snapshot.getConnectUrl());

        order.setTrackingUrl(snapshot.getTrackingUrl());
        order.setTrackingNumber(snapshot.getTrackingNumber());
        order.setTrackingCarrier(snapshot.getTrackingCarrier());
        order.setShipments(snapshot.getShipments());
        order.setTrackingLastSyncedAt(LocalDateTime.now());
    }

    private OrderTrackingResponse toTrackingResponse(Order order) {
        return OrderTrackingResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .displayStatus(displayStatus(order.getPrintifyStatus(), order.getStatus()))
                .totalAmount(order.getTotalAmount())

                .printifyOrderId(order.getPrintifyOrderId())
                .printifyStatus(order.getPrintifyStatus())
                .printifyConnectUrl(order.getPrintifyConnectUrl())

                .trackingUrl(order.getTrackingUrl())
                .trackingNumber(order.getTrackingNumber())
                .trackingCarrier(order.getTrackingCarrier())
                .trackingLastSyncedAt(order.getTrackingLastSyncedAt())

                .shippingFullName(order.getShippingFullName())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingCountry(order.getShippingCountry())

                .items(order.getItems())
                .shipments(order.getShipments())
                .build();
    }

    private String toLocalStatus(String printifyStatus) {
        if (printifyStatus == null || printifyStatus.isBlank()) {
            return "PROCESSING";
        }

        return switch (printifyStatus.toLowerCase()) {
            case "on-hold" -> "ON_HOLD";
            case "pending" -> "PENDING";
            case "in-production", "fulfilled" -> "IN_PRODUCTION";
            case "shipped" -> "SHIPPED";
            case "delivered" -> "DELIVERED";
            case "canceled", "cancelled" -> "CANCELLED";
            default -> "PROCESSING";
        };
    }

    private String displayStatus(String printifyStatus, String localStatus) {
        if (printifyStatus == null || printifyStatus.isBlank()) {
            return "Order received";
        }

        return switch (printifyStatus.toLowerCase()) {
            case "on-hold" -> "Waiting for production confirmation";
            case "pending" -> "Order received";
            case "in-production" -> "In production";
            case "fulfilled" -> "Production completed";
            case "shipped" -> "Shipped";
            case "delivered" -> "Delivered";
            case "canceled", "cancelled" -> "Cancelled";
            default -> localStatus;
        };
    }

    public Order checkoutAfterOnlinePayment(
            User user,
            CheckoutRequest request,
            String razorpayOrderId,
            String razorpayPaymentId
    ) {
        Order order = checkout(user, request);

        order.setPaymentMethod("RAZORPAY");
        order.setPaymentStatus("PAID");
        order.setRazorpayOrderId(razorpayOrderId);
        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaidCurrency("INR");
        order.setPaidAmount(order.getTotalAmount());

        return orderRepository.save(order);
    }

    public Order checkoutAfterVerifiedQuote(User user, CheckoutQuote quote) {
        if (!quote.getUserId().equals(user.getId())) {
            throw new BadRequestException("Invalid payment quote");
        }

        if (!"PAID".equals(quote.getStatus())) {
            throw new BadRequestException("Payment is not completed");
        }

        CheckoutRequest request;
        try {
            request = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(quote.getCheckoutSnapshotJson(), CheckoutRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid checkout snapshot");
        }

        shippingValidationService.validate(request);

        Order order = Order.builder()
                .userId(user.getId())
                .status("PENDING")
                .totalAmount(quote.getGrandTotal())
                .shippingFullName(request.getFullName())
                .shippingEmail(request.getEmail())
                .shippingPhone(request.getPhone())
                .shippingAddressLine1(request.getAddressLine1())
                .shippingAddressLine2(request.getAddressLine2())
                .shippingCity(request.getCity())
                .shippingState(request.getState())
                .shippingPostalCode(request.getPostalCode())
                .shippingCountry(request.getCountry())
                .items(quote.getItems())
                .paymentProvider("RAZORPAY")
                .paymentMethod("ONLINE")
                .paymentStatus("PAID")
                .paymentCurrency(quote.getPaymentCurrency())
                .paidAmount(quote.getGrandTotal())
                .paidAmountMinorUnit(quote.getAmountMinorUnit())
                .razorpayOrderId(quote.getRazorpayOrderId())
                .razorpayPaymentId(quote.getRazorpayPaymentId())
                .checkoutQuoteId(quote.getId())
                .build();

        order = orderRepository.save(order);

        PrintifyOrderSnapshot snapshot = printifyService.createOrder(order);

        applyPrintifySnapshot(order, snapshot);
        order.setStatus(toLocalStatus(snapshot.getStatus()));

        order = orderRepository.save(order);

        cartService.clearCart(user);

        return order;
    }
}