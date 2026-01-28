// HomeController.java
package com.project.parkminjeproject.global.controller;

import com.project.parkminjeproject.domain.portfolio.entity.Portfolio;
import com.project.parkminjeproject.domain.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 홈 컨트롤러 - 조회수 증가 기능 추가
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PortfolioService portfolioService;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "9") int size,
                       Model model) {

        // 페이징과 검색을 지원하는 공개 포트폴리오 조회
        Page<Portfolio> portfolioPage = portfolioService.searchPublishedPortfoliosWithPaging(
                search, category, page, size);

        model.addAttribute("portfolios", portfolioPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("category", category);

        // 페이징 정보
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", portfolioPage.getTotalPages());
        model.addAttribute("totalItems", portfolioPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNext", portfolioPage.hasNext());
        model.addAttribute("hasPrevious", portfolioPage.hasPrevious());

        return "common/index";
    }

    @GetMapping("/about")
    public String about() {
        return "common/about";
    }

    /**
     * 포트폴리오 상세 페이지 (조회수 증가 기능 추가)
     */
    @GetMapping("/portfolio/{id}")
    public String portfolioDetail(@PathVariable Long id,
                                  Model model,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);

            // 비공개 포트폴리오는 일반 사용자에게 보여주지 않음
            if (!portfolio.isPublished()) {
                log.warn("비공개 포트폴리오 접근 시도 - ID: {}, IP: {}",
                        id, getClientIP(request));
                return "redirect:/?error=notfound";
            }

            // 🆕 조회수 증가 (중복 방지)
            if (shouldIncrementViewCount(id, request)) {
                portfolioService.incrementViewCount(id);
                setViewCookie(id, response);
                log.debug("조회수 증가 - Portfolio ID: {}, 새 조회수: {}",
                        id, portfolio.getViewCount() + 1);
            }

            model.addAttribute("portfolio", portfolio);

            // 같은 카테고리의 다른 포트폴리오 추천 (최대 3개)
            List<Portfolio> relatedPortfolios = portfolioService
                    .getPublishedPortfoliosByCategory(portfolio.getCategory())
                    .stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(3)
                    .toList();

            model.addAttribute("relatedPortfolios", relatedPortfolios);

            return "portfolio/portfolio-detail";
        } catch (Exception e) {
            log.error("포트폴리오 조회 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return "redirect:/?error=notfound";
        }
    }

    /**
     * 조회수 증가 여부 확인 (쿠키 기반 중복 방지)
     * 같은 사용자가 24시간 내에 다시 방문하면 조회수 증가하지 않음
     */
    private boolean shouldIncrementViewCount(Long portfolioId, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return true;
        }

        String cookieName = "portfolio_view_" + portfolioId;
        return Arrays.stream(cookies)
                .noneMatch(cookie -> cookieName.equals(cookie.getName()));
    }

    /**
     * 조회 기록 쿠키 설정 (24시간 유효)
     */
    private void setViewCookie(Long portfolioId, HttpServletResponse response) {
        Cookie cookie = new Cookie("portfolio_view_" + portfolioId, "1");
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}