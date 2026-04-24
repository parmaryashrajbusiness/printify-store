package com.printify.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.printify.store.entity.Order;
import com.printify.store.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrintifyService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.printify.base-url}")
    private String baseUrl;

    @Value("${app.printify.token}")
    private String token;

    @Value("${app.printify.shop-id}")
    private String shopId;

    public String createOrder(Order order) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("external_id", order.getId());
        payload.put("label", "Store Order " + order.getId());
        payload.put("shipping_method", 1);
        payload.put("send_shipping_notification", false);

        Map<String, Object> addressTo = new HashMap<>();
        addressTo.put("first_name", getFirstName(order.getShippingFullName()));
        addressTo.put("last_name", getLastName(order.getShippingFullName()));
        addressTo.put("email", order.getShippingEmail());
        addressTo.put("phone", order.getShippingPhone());
        addressTo.put("country", order.getShippingCountry().toUpperCase());
        addressTo.put("region", order.getShippingState());
        addressTo.put("address1", order.getShippingAddressLine1());
        addressTo.put("address2", safe(order.getShippingAddressLine2()));
        addressTo.put("city", order.getShippingCity());
        addressTo.put("zip", order.getShippingPostalCode());

        payload.put("address_to", addressTo);

        List<Map<String, Object>> lineItems =
                order.getItems().stream().map(this::mapItem).toList();

        payload.put("line_items", lineItems);

        String response = webClient.post()
                .uri(baseUrl + "/shops/" + shopId + "/orders.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.USER_AGENT, "NeonCart")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode json = objectMapper.readTree(response);
            return json.path("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Printify response: " + response);
        }
    }

    private Map<String, Object> mapItem(OrderItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", item.getPrintifyProductId());
        map.put("variant_id", Integer.valueOf(item.getPrintifyVariantId()));
        map.put("quantity", item.getQuantity());
        return map;
    }

    private String getFirstName(String fullName) {
        String name = safe(fullName).replaceAll("\\s+", " ").trim();
        String[] parts = name.split(" ");
        return parts[0];
    }

    private String getLastName(String fullName) {
        String name = safe(fullName).replaceAll("\\s+", " ").trim();
        String[] parts = name.split(" ", 2);
        return parts.length > 1 ? parts[1] : parts[0];
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}