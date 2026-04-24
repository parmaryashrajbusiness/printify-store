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
        payload.put("send_shipping_notification", false);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("name", order.getShippingFullName());
        recipient.put("email", order.getShippingEmail());
        recipient.put("phone", order.getShippingPhone());
        recipient.put("country", order.getShippingCountry());
        recipient.put("region", order.getShippingState());
        recipient.put("address1", order.getShippingAddressLine1());
        recipient.put("address2", order.getShippingAddressLine2());
        recipient.put("city", order.getShippingCity());
        recipient.put("zip", order.getShippingPostalCode());

        payload.put("recipient", recipient);

        List<Map<String, Object>> lineItems = order.getItems().stream().map(this::mapItem).toList();
        payload.put("line_items", lineItems);

        String response = webClient.post()
                .uri(baseUrl + "/shops/" + shopId + "/orders.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode json = objectMapper.readTree(response);
            return json.path("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Printify response");
        }
    }

    private Map<String, Object> mapItem(OrderItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", item.getPrintifyProductId());
        map.put("variant_id", Integer.valueOf(item.getPrintifyVariantId()));
        map.put("quantity", item.getQuantity());
        return map;
    }
}