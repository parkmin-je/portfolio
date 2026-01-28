package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.service.PortfolioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 좋아요 기능 컨트롤러
 * 쿠키 기반 중복 방지 (24시간)
 */
@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class LikeController {

    private final PortfolioService portfolioService;

    /**
     * 좋아요 토글 (추가/취소)
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response) {

        Map<String, Object> result = new HashMap<>();
        String cookieName = "portfolio_like_" + id;

        try {
            // 이미 좋아요를 눌렀는지 확인
            boolean alreadyLiked = hasLiked(id, request);

            if (alreadyLiked) {
                // ✅ 좋아요 취소 - DB 카운트 감소 추가
                portfolioService.decrementLikeCount(id);

                // 쿠키 삭제
                Cookie cookie = new Cookie(cookieName, "");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);

                result.put("action", "unliked");
                result.put("message", "좋아요를 취소했습니다.");
                log.debug("좋아요 취소 - Portfolio ID: {}", id);

            } else {
                // 좋아요 추가
                portfolioService.incrementLikeCount(id);

                // 쿠키 설정 (24시간)
                Cookie cookie = new Cookie(cookieName, "1");
                cookie.setMaxAge(24 * 60 * 60);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                response.addCookie(cookie);

                result.put("action", "liked");
                result.put("message", "좋아요를 눌렀습니다.");
                log.debug("좋아요 추가 - Portfolio ID: {}", id);
            }

            // 현재 좋아요 수 반환
            Long likeCount = portfolioService.getPortfolioById(id).getLikeCount();
            result.put("likeCount", likeCount);
            result.put("success", true);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("좋아요 처리 실패 - Portfolio ID: {}, 오류: {}", id, e.getMessage());
            result.put("success", false);
            result.put("message", "좋아요 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 좋아요 상태 확인
     */
    @GetMapping("/{id}/like-status")
    public ResponseEntity<Map<String, Object>> getLikeStatus(
            @PathVariable Long id,
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();

        try {
            boolean hasLiked = hasLiked(id, request);
            Long likeCount = portfolioService.getPortfolioById(id).getLikeCount();

            result.put("hasLiked", hasLiked);
            result.put("likeCount", likeCount);
            result.put("success", true);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("좋아요 상태 조회 실패 - Portfolio ID: {}", id);
            result.put("success", false);
            result.put("message", "조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 이미 좋아요를 눌렀는지 확인 (쿠키 체크)
     */
    private boolean hasLiked(Long portfolioId, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        String cookieName = "portfolio_like_" + portfolioId;
        return Arrays.stream(cookies)
                .anyMatch(cookie -> cookieName.equals(cookie.getName()));
    }
}