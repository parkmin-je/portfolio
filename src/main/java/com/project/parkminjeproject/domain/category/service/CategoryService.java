package com.project.parkminjeproject.domain.category.service;

import com.project.parkminjeproject.domain.category.entity.Category;
import com.project.parkminjeproject.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 모든 카테고리 조회 (활성화된 것만, 순서대로)
     */
    @Cacheable("categories")
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * 모든 카테고리 조회 (관리자용)
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 카테고리 조회
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
    }

    /**
     * 카테고리 이름으로 조회
     */
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElse(null);
    }

    /**
     * 카테고리 저장
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category saveCategory(Category category) {
        // 중복 체크
        if (category.getId() == null) {
            Category existing = getCategoryByName(category.getName());
            if (existing != null) {
                throw new RuntimeException("이미 존재하는 카테고리입니다.");
            }
        }
        return categoryRepository.save(category);
    }

    /**
     * 카테고리 삭제
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    /**
     * 카테고리 활성화/비활성화
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void toggleActive(Long id) {
        Category category = getCategoryById(id);
        category.setActive(!category.isActive());
        categoryRepository.save(category);
    }

    /**
     * 기본 카테고리 초기화 (애플리케이션 시작 시)
     */
    @Transactional
    public void initializeDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return; // 이미 카테고리가 있으면 스킵
        }

        String[] defaultCategories = {
                "웹 개발", "앱 개발", "디자인", "데이터 분석", "AI/ML", "기타"
        };

        for (int i = 0; i < defaultCategories.length; i++) {
            Category category = new Category();
            category.setName(defaultCategories[i]);
            category.setDisplayOrder(i);
            category.setActive(true);
            categoryRepository.save(category);
        }

        log.info("기본 카테고리 초기화 완료");
    }
}