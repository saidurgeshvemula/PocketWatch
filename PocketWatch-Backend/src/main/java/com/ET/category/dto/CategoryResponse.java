package com.ET.category.dto;

import com.ET.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor()
        );
    }
}
