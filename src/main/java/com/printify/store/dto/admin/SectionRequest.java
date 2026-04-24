package com.printify.store.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SectionRequest {
    @NotBlank
    private String name;

    private String subtitle;
    private String highlight;

    @NotNull
    private Boolean visible;

    @NotNull
    private Integer sortOrder;
}