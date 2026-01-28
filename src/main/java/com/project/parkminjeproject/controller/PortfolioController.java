package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.audit.AuditLogService;
import com.project.parkminjeproject.entity.Portfolio;
import com.project.parkminjeproject.entity.User;
import com.project.parkminjeproject.service.FileUploadService;
import com.project.parkminjeproject.service.PortfolioService;
import com.project.parkminjeproject.service.UrlValidationService;
import com.project.parkminjeproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("/admin/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;
    private final UrlValidationService urlValidationService;
    private final UserService userService;  // ✅ 추가

    @GetMapping
    public String portfolioList(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String category,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {

        Page<Portfolio> portfolioPage = portfolioService.searchPortfoliosWithPaging(
                search, category, page, size);

        model.addAttribute("portfolios", portfolioPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", portfolioPage.getTotalPages());
        model.addAttribute("totalItems", portfolioPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNext", portfolioPage.hasNext());
        model.addAttribute("hasPrevious", portfolioPage.hasPrevious());

        return "admin/portfolio-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("portfolio", new Portfolio());
        return "admin/portfolio-form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Portfolio portfolio,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         RedirectAttributes redirectAttributes) {
        try {
            log.info("========================================");
            log.info("포트폴리오 생성 시작");

            // ✅ 1. 현재 로그인한 사용자 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            log.info("로그인 사용자: {}", username);

            // ✅ 2. 사용자가 DB에 존재하는지 확인 (중요!)
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("❌ 사용자를 찾을 수 없음: {}", username);
                        return new RuntimeException("사용자를 찾을 수 없습니다: " + username);
                    });

            log.info("✅ 사용자 확인 완료 - userId: {}, username: {}, role: {}",
                    user.getId(), user.getUsername(), user.getRole());

            // ✅ 3. 포트폴리오에 사용자 설정 (Foreign Key 오류 방지)
            portfolio.setUser(user);
            log.info("✅ Portfolio에 User 설정 완료 - user_id: {}", user.getId());

            // 4. URL 검증
            if (!validateUrls(portfolio, redirectAttributes)) {
                return "redirect:/admin/portfolio/create";
            }

            // 5. 이미지 파일 업로드 처리
            if (imageFile != null && !imageFile.isEmpty()) {
                if (fileUploadService.isImageFile(imageFile)) {
                    String imageUrl = fileUploadService.uploadFile(imageFile);
                    portfolio.setImageUrl(imageUrl);
                    log.info("이미지 업로드 완료: {}", imageUrl);
                } else {
                    redirectAttributes.addFlashAttribute("error",
                            "이미지 파일만 업로드 가능합니다.");
                    return "redirect:/admin/portfolio/create";
                }
            }

            // 6. 저장
            Portfolio savedPortfolio = portfolioService.savePortfolio(portfolio);
            log.info("✅ 포트폴리오 저장 완료 - portfolioId: {}, userId: {}",
                    savedPortfolio.getId(), savedPortfolio.getUser().getId());

            // 7. 감사 로그
            auditLogService.logPortfolioCreated(savedPortfolio.getId(), savedPortfolio.getTitle());

            log.info("포트폴리오 생성 성공 - ID: {}, 제목: {}",
                    savedPortfolio.getId(), savedPortfolio.getTitle());
            log.info("========================================");

            redirectAttributes.addFlashAttribute("message", "포트폴리오가 등록되었습니다.");
            return "redirect:/admin/portfolio";

        } catch (RuntimeException e) {
            log.error("========================================");
            log.error("❌ 포트폴리오 생성 실패 (RuntimeException): {}", e.getMessage(), e);
            log.error("========================================");
            redirectAttributes.addFlashAttribute("error",
                    "포트폴리오 생성 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/portfolio/create";

        } catch (IOException e) {
            log.error("========================================");
            log.error("❌ 파일 업로드 실패: {}", e.getMessage(), e);
            log.error("========================================");
            redirectAttributes.addFlashAttribute("error",
                    "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/portfolio/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Portfolio portfolio = portfolioService.getPortfolioById(id);
        model.addAttribute("portfolio", portfolio);
        return "admin/portfolio-form";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute Portfolio portfolio,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         RedirectAttributes redirectAttributes) {
        try {
            log.info("========================================");
            log.info("포트폴리오 수정 시작 - portfolioId: {}", portfolio.getId());

            // ✅ 1. 기존 포트폴리오 조회 (user_id 유지를 위해)
            Portfolio existingPortfolio = portfolioService.getPortfolioById(portfolio.getId());
            log.info("기존 Portfolio 조회 - userId: {}", existingPortfolio.getUser().getId());

            // ✅ 2. 기존 사용자 정보 유지 (중요!)
            portfolio.setUser(existingPortfolio.getUser());
            log.info("✅ 기존 User 정보 유지 - user_id: {}", portfolio.getUser().getId());

            // 3. URL 검증
            if (!validateUrls(portfolio, redirectAttributes)) {
                return "redirect:/admin/portfolio/edit/" + portfolio.getId();
            }

            // 4. 이미지 파일 업로드 처리
            if (imageFile != null && !imageFile.isEmpty()) {
                if (fileUploadService.isImageFile(imageFile)) {
                    String imageUrl = fileUploadService.uploadFile(imageFile);
                    portfolio.setImageUrl(imageUrl);
                    log.info("이미지 업로드 완료: {}", imageUrl);
                } else {
                    redirectAttributes.addFlashAttribute("error",
                            "이미지 파일만 업로드 가능합니다.");
                    return "redirect:/admin/portfolio/edit/" + portfolio.getId();
                }
            } else {
                // 이미지 파일이 없으면 기존 이미지 유지
                portfolio.setImageUrl(existingPortfolio.getImageUrl());
            }

            // 5. 저장
            Portfolio updatedPortfolio = portfolioService.savePortfolio(portfolio);
            log.info("✅ 포트폴리오 수정 완료 - portfolioId: {}", updatedPortfolio.getId());

            // 6. 감사 로그
            auditLogService.logPortfolioUpdated(updatedPortfolio.getId(), updatedPortfolio.getTitle());

            log.info("포트폴리오 수정 성공 - ID: {}, 제목: {}",
                    updatedPortfolio.getId(), updatedPortfolio.getTitle());
            log.info("========================================");

            redirectAttributes.addFlashAttribute("message", "포트폴리오가 수정되었습니다.");
            return "redirect:/admin/portfolio";

        } catch (RuntimeException e) {
            log.error("========================================");
            log.error("❌ 포트폴리오 수정 실패 (RuntimeException): {}", e.getMessage(), e);
            log.error("========================================");
            redirectAttributes.addFlashAttribute("error",
                    "포트폴리오 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/portfolio";

        } catch (IOException e) {
            log.error("========================================");
            log.error("❌ 파일 업로드 실패: {}", e.getMessage(), e);
            log.error("========================================");
            redirectAttributes.addFlashAttribute("error",
                    "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/portfolio/edit/" + portfolio.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);
            String title = portfolio.getTitle();

            portfolioService.deletePortfolio(id);
            auditLogService.logPortfolioDeleted(id, title);

            log.info("포트폴리오 삭제 - ID: {}, 제목: {}", id, title);

            redirectAttributes.addFlashAttribute("message", "포트폴리오가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("포트폴리오 삭제 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/portfolio";
    }

    @PostMapping("/toggle-publish/{id}")
    public String togglePublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            portfolioService.togglePublished(id);
            redirectAttributes.addFlashAttribute("message", "공개 상태가 변경되었습니다.");
        } catch (Exception e) {
            log.error("공개 상태 변경 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "상태 변경 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/portfolio";
    }

    /**
     * URL 검증 (기존 UrlValidationService에 맞춤)
     */
    private boolean validateUrls(Portfolio portfolio, RedirectAttributes redirectAttributes) {

        // 1. 이미지 URL 검증
        if (portfolio.getImageUrl() != null && !portfolio.getImageUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateImageUrl(portfolio.getImageUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error",
                        "이미지 URL 오류: " + result.getMessage());
                return false;
            }
        }

        // 2. GitHub URL 검증
        if (portfolio.getGithubUrl() != null && !portfolio.getGithubUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getGithubUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error",
                        "GitHub URL 오류: " + result.getMessage());
                return false;
            }
        }

        // 3. Demo URL 검증
        if (portfolio.getDemoUrl() != null && !portfolio.getDemoUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getDemoUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error",
                        "데모 URL 오류: " + result.getMessage());
                return false;
            }
        }

        // 4. Project URL 검증
        if (portfolio.getProjectUrl() != null && !portfolio.getProjectUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getProjectUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error",
                        "프로젝트 URL 오류: " + result.getMessage());
                return false;
            }
        }

        return true;
    }
}