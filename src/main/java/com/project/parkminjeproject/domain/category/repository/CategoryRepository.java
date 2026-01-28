package com.project.parkminjeproject.domain.category.repository;

import com.project.parkminjeproject.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 활성화된 카테고리만 순서대로 조회
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    // 모든 카테고리를 표시순서대로 조회
    List<Category> findAllByOrderByDisplayOrderAsc();

    // 이름으로 조회
    Optional<Category> findByName(String name);
}
