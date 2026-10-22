package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.CategoryResponse;
import com.smartbudget.tracker.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() { return categories.findAll().stream().map(CategoryResponse::from).toList(); }
}
