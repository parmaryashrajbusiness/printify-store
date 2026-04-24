package com.printify.store.controller;

import com.printify.store.dto.admin.SectionRequest;
import com.printify.store.entity.ProductSection;
import com.printify.store.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sections")
@RequiredArgsConstructor
public class AdminSectionController {

    private final SectionService sectionService;

    @GetMapping
    public List<ProductSection> getAll() {
        return sectionService.getAll();
    }

    @PostMapping
    public ProductSection create(@Valid @RequestBody SectionRequest request) {
        return sectionService.create(request);
    }

    @PutMapping("/{id}")
    public ProductSection update(@PathVariable String id, @Valid @RequestBody SectionRequest request) {
        return sectionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        sectionService.delete(id);
    }
}