package com.printify.store.controller;

import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.dto.order.OrderTrackingResponse;
import com.printify.store.entity.Order;
import com.printify.store.entity.User;
import com.printify.store.service.CurrentUserService;
import com.printify.store.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<Order> getOrders(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return orderService.getOrders(user);
    }

    @GetMapping("/{orderId}/tracking")
    public OrderTrackingResponse getTracking(
            Authentication authentication,
            @PathVariable String orderId
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        return orderService.getTracking(user, orderId);
    }

    @PostMapping("/checkout")
    public Order checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        return orderService.checkout(user, request);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderTrackingResponse cancelOrder(
            Authentication authentication,
            @PathVariable String orderId
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        return orderService.cancelCustomerOrder(user, orderId);
    }

    @DeleteMapping("/{orderId}")
    public void hideOrder(
            Authentication authentication,
            @PathVariable String orderId
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        orderService.hideCustomerOrder(user, orderId);
    }

}