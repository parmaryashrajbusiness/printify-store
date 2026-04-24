package com.printify.store.service;

import com.printify.store.dto.order.CheckoutRequest;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.Order;
import com.printify.store.entity.OrderItem;
import com.printify.store.entity.Product;
import com.printify.store.entity.User;
import com.printify.store.exception.BadRequestException;
import com.printify.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        List<OrderItem> items = cartItems.stream().map(cartItem -> {
            Product product = productService.getById(cartItem.getProductId());

            return OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .imageUrl(product.getImageUrl())
                    .colorway(product.getColorway())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .printifyProductId(product.getPrintifyProductId())
                    .printifyVariantId(product.getPrintifyVariantId())
                    .build();
        }).toList();

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

        String printifyOrderId = printifyService.createOrder(order);
        order.setPrintifyOrderId(printifyOrderId);
        order.setStatus("PROCESSING");

        order = orderRepository.save(order);
        cartService.clearCart(user);

        return order;
    }
}