package com.project.parkminjeproject.domain.portfolio.service;

import com.project.parkminjeproject.domain.portfolio.entity.Portfolio;
import com.project.parkminjeproject.domain.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    // ========== 사용자용 메서드 (공개된 것만) ==========

    public List<Portfolio> getPublishedPortfolios() {
        return portfolioRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Portfolio> getPublishedPortfoliosByCategory(String category) {
        return portfolioRepository.findByCategoryAndPublishedTrue(category);
    }

    // ========== 관리자용 메서드 (전체 조회) ==========

    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Portfolio> getPortfoliosByCategory(String category) {
        return portfolioRepository.findByCategory(category);
    }

    public Portfolio getPortfolioById(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 🔧 수정: 트랜잭션 격리 수준 명시 및 플러시 추가
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio savePortfolio(Portfolio portfolio) {
        try {
            // 수정인 경우 기존 엔티티를 먼저 조회하여 영속성 컨텍스트에 로드
            if (portfolio.getId() != null) {
                Portfolio existingPortfolio = portfolioRepository.findById(portfolio.getId())
                        .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다. ID: " + portfolio.getId()));

                // 기존 엔티티의 값을 업데이트 (merge 방식)
                existingPortfolio.setTitle(portfolio.getTitle());
                existingPortfolio.setCategory(portfolio.getCategory());
                existingPortfolio.setSummary(portfolio.getSummary());
                existingPortfolio.setDescription(portfolio.getDescription());
                existingPortfolio.setImageUrl(portfolio.getImageUrl());
                existingPortfolio.setProjectUrl(portfolio.getProjectUrl());
                existingPortfolio.setGithubUrl(portfolio.getGithubUrl());
                existingPortfolio.setDemoUrl(portfolio.getDemoUrl());
                existingPortfolio.setTechnologies(portfolio.getTechnologies());
                existingPortfolio.setDuration(portfolio.getDuration());
                existingPortfolio.setRole(portfolio.getRole());
                existingPortfolio.setClient(portfolio.getClient());
                existingPortfolio.setTeamSize(portfolio.getTeamSize());
                existingPortfolio.setAchievements(portfolio.getAchievements());
                existingPortfolio.setChallenges(portfolio.getChallenges());
                existingPortfolio.setImpact(portfolio.getImpact());
                existingPortfolio.setPublished(portfolio.isPublished());
                existingPortfolio.setFeatured(portfolio.isFeatured());
                existingPortfolio.setDisplayOrder(portfolio.getDisplayOrder());

                Portfolio savedPortfolio = portfolioRepository.saveAndFlush(existingPortfolio);
                log.info("포트폴리오 수정 완료 - ID: {}, 제목: {}", savedPortfolio.getId(), savedPortfolio.getTitle());
                return savedPortfolio;
            } else {
                // 신규 생성
                Portfolio savedPortfolio = portfolioRepository.saveAndFlush(portfolio);
                log.info("포트폴리오 생성 완료 - ID: {}, 제목: {}", savedPortfolio.getId(), savedPortfolio.getTitle());
                return savedPortfolio;
            }
        } catch (Exception e) {
            log.error("포트폴리오 저장 실패 - ID: {}, 에러: {}", portfolio.getId(), e.getMessage(), e);
            throw new RuntimeException("포트폴리오 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 🔧 수정: 트랜잭션 격리 수준 명시 및 존재 여부 확인 추가
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletePortfolio(Long id) {
        try {
            // 먼저 존재 여부 확인
            if (!portfolioRepository.existsById(id)) {
                throw new RuntimeException("삭제할 포트폴리오를 찾을 수 없습니다. ID: " + id);
            }

            portfolioRepository.deleteById(id);
            portfolioRepository.flush(); // 즉시 DB에 반영
            log.info("포트폴리오 삭제 완료 - ID: {}", id);
        } catch (Exception e) {
            log.error("포트폴리오 삭제 실패 - ID: {}, 에러: {}", id, e.getMessage(), e);
            throw new RuntimeException("포트폴리오 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 🔧 수정: 낙관적 락 추가 고려
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void togglePublished(Long id) {
        try {
            Portfolio portfolio = portfolioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다. ID: " + id));

            portfolio.setPublished(!portfolio.isPublished());
            portfolioRepository.saveAndFlush(portfolio);

            log.info("포트폴리오 공개 상태 변경 완료 - ID: {}, 공개여부: {}", id, portfolio.isPublished());
        } catch (Exception e) {
            log.error("포트폴리오 상태 변경 실패 - ID: {}, 에러: {}", id, e.getMessage(), e);
            throw new RuntimeException("포트폴리오 상태 변경 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long id) {
        Portfolio portfolio = getPortfolioById(id);
        portfolio.incrementViewCount();
        portfolioRepository.saveAndFlush(portfolio);
        log.debug("조회수 증가 - Portfolio ID: {}, 조회수: {}", id, portfolio.getViewCount());
    }

    /**
     * 좋아요 수 증가
     */
    @Transactional
    public void incrementLikeCount(Long id) {
        Portfolio portfolio = getPortfolioById(id);
        portfolio.incrementLikeCount();
        portfolioRepository.saveAndFlush(portfolio);
        log.debug("좋아요 증가 - Portfolio ID: {}, 좋아요: {}", id, portfolio.getLikeCount());
    }
    @Transactional
    public void decrementLikeCount(Long id) {
        Portfolio portfolio = getPortfolioById(id);
        portfolio.decrementLikeCount();
        portfolioRepository.save(portfolio);
        log.debug("좋아요 감소 - Portfolio ID: {}, 좋아요: {}", id, portfolio.getLikeCount());
    }
    // PortfolioService.java에 다음 메서드를 추가하세요

    /**
     * 특정 사용자의 포트폴리오 조회 (페이징)
     */
    public Page<Portfolio> getPortfoliosByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return portfolioRepository.findByUserId(userId, pageable);
    }

    // ========== 페이징 메서드 (관리자용) ==========

    public Page<Portfolio> searchPortfoliosWithPaging(String search, String category,
                                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.trim().isEmpty() &&
                category != null && !category.trim().isEmpty()) {
            return portfolioRepository.findByTitleContainingIgnoreCaseAndCategory(
                    search, category, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            return portfolioRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            return portfolioRepository.findByCategory(category, pageable);
        } else {
            return portfolioRepository.findAll(pageable);
        }
    }

    public Page<Portfolio> searchPublishedPortfoliosWithPaging(String search, String category,
                                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.trim().isEmpty() &&
                category != null && !category.trim().isEmpty()) {
            return portfolioRepository.findByTitleContainingIgnoreCaseAndCategoryAndPublishedTrue(
                    search, category, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            return portfolioRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(
                    search, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            return portfolioRepository.findByCategoryAndPublishedTrue(category, pageable);
        } else {
            return portfolioRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
        }
    }

    // ========== 대시보드 통계 메서드 ==========

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Portfolio> allPortfolios = portfolioRepository.findAll();

        // 전체 개수
        long totalCount = allPortfolios.size();
        stats.put("totalPortfolios", totalCount);

        // 공개/비공개 개수
        long publishedCount = allPortfolios.stream()
                .filter(Portfolio::isPublished).count();
        long unpublishedCount = totalCount - publishedCount;
        stats.put("publishedPortfolios", publishedCount);
        stats.put("unpublishedPortfolios", unpublishedCount);

        // 카테고리별 통계
        Map<String, Long> categoryStats = allPortfolios.stream()
                .collect(Collectors.groupingBy(
                        Portfolio::getCategory,
                        Collectors.counting()
                ));
        stats.put("categoryStats", categoryStats);

        // 최근 등록된 포트폴리오 5개
        List<Portfolio> recentPortfolios = allPortfolios.stream()
                .filter(p -> p.getCreatedAt() != null)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        stats.put("recentPortfolios", recentPortfolios);

        // 총 조회수
        long totalViews = allPortfolios.stream()
                .mapToLong(Portfolio::getViewCount)
                .sum();
        stats.put("totalViews", totalViews);

        // 총 좋아요 수
        long totalLikes = allPortfolios.stream()
                .mapToLong(Portfolio::getLikeCount)
                .sum();
        stats.put("totalLikes", totalLikes);

        // 가장 인기있는 포트폴리오 (조회수 기준)
        Portfolio mostViewed = allPortfolios.stream()
                .filter(p -> p.getViewCount() > 0)
                .max((p1, p2) -> Long.compare(p1.getViewCount(), p2.getViewCount()))
                .orElse(null);
        stats.put("mostViewedPortfolio", mostViewed);

        return stats;
    }

    /**
     * 인기 포트폴리오 조회 (조회수 상위 N개)
     */
    public List<Portfolio> getTopViewedPortfolios(int limit) {
        return portfolioRepository.findByPublishedTrueOrderByCreatedAtDesc()
                .stream()
                .sorted((p1, p2) -> Long.compare(p2.getViewCount(), p1.getViewCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 최근 업데이트된 포트폴리오 조회
     */
    public List<Portfolio> getRecentlyUpdatedPortfolios(int limit) {
        return getAllPortfolios().stream()
                .filter(p -> p.getUpdatedAt() != null)
                .sorted((p1, p2) -> p2.getUpdatedAt().compareTo(p1.getUpdatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}