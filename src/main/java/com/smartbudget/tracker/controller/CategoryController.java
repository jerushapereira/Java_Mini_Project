package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.CategoryResponse;
import com.smartbudget.tracker.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categories;
    public CategoryController(CategoryService categories) { this.categories = categories; }
    @GetMapping
    public List<CategoryResponse> list() { return categories.list(); }
}
