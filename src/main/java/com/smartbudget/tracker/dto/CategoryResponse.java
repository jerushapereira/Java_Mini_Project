package com.smartbudget.tracker.dto;

import com.smartbudget.tracker.entity.CategoryEntity;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(CategoryEntity category) { return new CategoryResponse(category.getId(), category.getName()); }
}
