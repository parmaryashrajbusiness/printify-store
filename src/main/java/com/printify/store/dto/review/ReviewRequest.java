package com.printify.store.dto.review;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(min = 5, max = 600)
    private String comment;
}