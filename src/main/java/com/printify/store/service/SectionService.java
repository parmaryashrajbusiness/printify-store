package com.printify.store.service;

import com.printify.store.dto.admin.SectionRequest;
import com.printify.store.entity.ProductSection;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.ProductSectionRepository;
import com.printify.store.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final ProductSectionRepository sectionRepository;

    public List<ProductSection> getVisibleSections() {
        return sectionRepository.findAllByVisibleTrueOrderBySortOrderAsc();
    }

    public List<ProductSection> getAll() {
        return sectionRepository.findAll();
    }

    public ProductSection create(SectionRequest request) {
        ProductSection section = ProductSection.builder()
                .name(request.getName())
                .slug(SlugUtil.toSlug(request.getName()))
                .subtitle(request.getSubtitle())
                .highlight(request.getHighlight())
                .visible(request.getVisible())
                .sortOrder(request.getSortOrder())
                .build();

        return sectionRepository.save(section);
    }

    public ProductSection update(String id, SectionRequest request) {
        ProductSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        section.setName(request.getName());
        section.setSlug(SlugUtil.toSlug(request.getName()));
        section.setSubtitle(request.getSubtitle());
        section.setHighlight(request.getHighlight());
        section.setVisible(request.getVisible());
        section.setSortOrder(request.getSortOrder());

        return sectionRepository.save(section);
    }

    public void delete(String id) {
        sectionRepository.deleteById(id);
    }

    public ProductSection getById(String id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }
}