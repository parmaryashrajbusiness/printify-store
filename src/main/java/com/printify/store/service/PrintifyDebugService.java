package com.printify.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class PrintifyDebugService {

    private final WebClient webClient;

    @Value("${app.printify.base-url}")
    private String baseUrl;

    @Value("${app.printify.token}")
    private String token;

    @Value("${app.printify.shop-id}")
    private String shopId;

    public String getShops() {
        return webClient.get()
                .uri(baseUrl + "/shops.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.USER_AGENT, "NeonCart-SpringBoot")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String getProducts() {
        return webClient.get()
                .uri(baseUrl + "/shops/" + shopId + "/products.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.USER_AGENT, "NeonCart-SpringBoot")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}