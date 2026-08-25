package com.ET.category.service;

import com.ET.category.dto.CategoryResponse;
import com.ET.category.repository.CategoryRepository;
import com.ET.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable("categories")
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
