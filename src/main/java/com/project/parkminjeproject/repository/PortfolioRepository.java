package com.project.parkminjeproject.repository;

import com.project.parkminjeproject.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    // 기본 조회 (공개된 것만)
    List<Portfolio> findByPublishedTrueOrderByCreatedAtDesc();
    List<Portfolio> findByCategoryAndPublishedTrue(String category);

    // 관리자용 전체 조회
    List<Portfolio> findByCategory(String category);
    List<Portfolio> findAllByOrderByCreatedAtDesc();
    Page<Portfolio> findByUserId(Long userId, Pageable pageable);

    // 페이징 지원 메서드 (관리자용)
    Page<Portfolio> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Portfolio> findByCategory(String category, Pageable pageable);
    Page<Portfolio> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Portfolio> findByTitleContainingIgnoreCaseAndCategory(String title, String category, Pageable pageable);

    // 페이징 지원 메서드 (공개된 것만)
    Page<Portfolio> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    Page<Portfolio> findByCategoryAndPublishedTrue(String category, Pageable pageable);
    Page<Portfolio> findByTitleContainingIgnoreCaseAndPublishedTrue(String title, Pageable pageable);
    Page<Portfolio> findByTitleContainingIgnoreCaseAndCategoryAndPublishedTrue(String title, String category, Pageable pageable);
}