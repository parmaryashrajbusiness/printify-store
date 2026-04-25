package com.printify.store.controller;

import com.printify.store.dto.contact.ContactRequest;
import com.printify.store.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public Map<String, String> sendContactMessage(@Valid @RequestBody ContactRequest request) {
        contactService.sendContactMessage(request);
        return Map.of("message", "Message sent successfully");
    }
}