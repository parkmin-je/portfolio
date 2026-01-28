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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("/my-portfolio")
@RequiredArgsConstructor
public class MyPortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;
    private final UrlValidationService urlValidationService;

    /**
     * 내 포트폴리오 목록
     */
    @GetMapping
    public String myPortfolioList(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername());

        // 내가 작성한 포트폴리오만 조회
        Page<Portfolio> portfolioPage = portfolioService.getPortfoliosByUser(currentUser.getId(), page, size);

        model.addAttribute("portfolios", portfolioPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", portfolioPage.getTotalPages());
        model.addAttribute("totalItems", portfolioPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNext", portfolioPage.hasNext());
        model.addAttribute("hasPrevious", portfolioPage.hasPrevious());
        model.addAttribute("userName", currentUser.getName());

        return "my-portfolio/list";
    }

    /**
     * 포트폴리오 작성 폼
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("portfolio", new Portfolio());
        return "my-portfolio/form";
    }

    /**
     * 포트폴리오 작성
     */
    @PostMapping("/create")
    public String create(@ModelAttribute Portfolio portfolio,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            log.info("포트폴리오 생성 시작 - 제목: {}", portfolio.getTitle());

            // URL 검증
            if (!validateUrls(portfolio, redirectAttributes)) {
                return "redirect:/my-portfolio/create";
            }

            // 이미지 파일 업로드
            if (imageFile != null && !imageFile.isEmpty()) {
                if (fileUploadService.isImageFile(imageFile)) {
                    String imageUrl = fileUploadService.uploadFile(imageFile);
                    portfolio.setImageUrl(imageUrl);
                } else {
                    redirectAttributes.addFlashAttribute("error", "이미지 파일만 업로드 가능합니다.");
                    return "redirect:/my-portfolio/create";
                }
            }

            // 현재 사용자를 작성자로 설정
            User currentUser = userService.getUserByUsername(userDetails.getUsername());
            portfolio.setUser(currentUser);

            Portfolio savedPortfolio = portfolioService.savePortfolio(portfolio);

            auditLogService.logPortfolioCreated(savedPortfolio.getId(), savedPortfolio.getTitle());
            log.info("✅ 포트폴리오 생성 성공 - ID: {}, 작성자: {}",
                    savedPortfolio.getId(), currentUser.getUsername());

            redirectAttributes.addFlashAttribute("message", "포트폴리오가 등록되었습니다.");
            return "redirect:/my-portfolio";

        } catch (IOException e) {
            log.error("❌ 파일 업로드 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
            return "redirect:/my-portfolio/create";
        } catch (Exception e) {
            log.error("❌ 포트폴리오 생성 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "포트폴리오 생성 중 오류가 발생했습니다.");
            return "redirect:/my-portfolio/create";
        }
    }

    /**
     * 포트폴리오 수정 폼
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);
            User currentUser = userService.getUserByUsername(userDetails.getUsername());

            // 권한 체크: 본인 것만 수정 가능
            if (!portfolio.isOwnedBy(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "본인의 포트폴리오만 수정할 수 있습니다.");
                return "redirect:/my-portfolio";
            }

            model.addAttribute("portfolio", portfolio);
            return "my-portfolio/form";
        } catch (Exception e) {
            log.error("❌ 포트폴리오 조회 실패 - ID: {}", id);
            redirectAttributes.addFlashAttribute("error", "포트폴리오를 찾을 수 없습니다.");
            return "redirect:/my-portfolio";
        }
    }

    /**
     * 포트폴리오 수정
     */
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Portfolio portfolio,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Portfolio existingPortfolio = portfolioService.getPortfolioById(id);
                User currentUser = userService.getUserByUsername(userDetails.getUsername());

                // 권한 체크
                if (!existingPortfolio.isOwnedBy(currentUser)) {
                    redirectAttributes.addFlashAttribute("error", "본인의 포트폴리오만 수정할 수 있습니다.");
                    return "redirect:/my-portfolio";
                }

                // URL 검증
                if (!validateUrls(portfolio, redirectAttributes)) {
                    return "redirect:/my-portfolio/edit/" + id;
                }

                // 이미지 처리
                if (imageFile != null && !imageFile.isEmpty()) {
                    if (fileUploadService.isImageFile(imageFile)) {
                        if (existingPortfolio.getImageUrl() != null &&
                                existingPortfolio.getImageUrl().startsWith("/uploads/")) {
                            fileUploadService.deleteFile(existingPortfolio.getImageUrl());
                        }
                        String imageUrl = fileUploadService.uploadFile(imageFile);
                        portfolio.setImageUrl(imageUrl);
                    } else {
                        redirectAttributes.addFlashAttribute("error", "이미지 파일만 업로드 가능합니다.");
                        return "redirect:/my-portfolio/edit/" + id;
                    }
                } else {
                    portfolio.setImageUrl(existingPortfolio.getImageUrl());
                }

                portfolio.setId(id);
                portfolio.setUser(existingPortfolio.getUser());

                Portfolio savedPortfolio = portfolioService.savePortfolio(portfolio);

                auditLogService.logPortfolioUpdated(id, portfolio.getTitle());
                log.info("✅ 포트폴리오 수정 성공 - ID: {}, 작성자: {}",
                        id, currentUser.getUsername());

                redirectAttributes.addFlashAttribute("message", "포트폴리오가 수정되었습니다.");
                return "redirect:/my-portfolio";

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    redirectAttributes.addFlashAttribute("error", "잠시 후 다시 시도해주세요.");
                    return "redirect:/my-portfolio/edit/" + id;
                }
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                log.error("❌ 파일 업로드 실패: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
                return "redirect:/my-portfolio/edit/" + id;
            } catch (Exception e) {
                log.error("❌ 포트폴리오 수정 실패: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("error", "포트폴리오 수정 중 오류가 발생했습니다.");
                return "redirect:/my-portfolio/edit/" + id;
            }
        }

        redirectAttributes.addFlashAttribute("error", "포트폴리오 수정에 실패했습니다.");
        return "redirect:/my-portfolio/edit/" + id;
    }

    /**
     * 포트폴리오 삭제
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);
            User currentUser = userService.getUserByUsername(userDetails.getUsername());

            // 권한 체크
            if (!portfolio.isOwnedBy(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "본인의 포트폴리오만 삭제할 수 있습니다.");
                return "redirect:/my-portfolio";
            }

            // 이미지 파일 삭제
            if (portfolio.getImageUrl() != null &&
                    portfolio.getImageUrl().startsWith("/uploads/")) {
                fileUploadService.deleteFile(portfolio.getImageUrl());
            }

            portfolioService.deletePortfolio(id);

            auditLogService.logPortfolioDeleted(id, portfolio.getTitle());
            log.info("✅ 포트폴리오 삭제 성공 - ID: {}, 작성자: {}",
                    id, currentUser.getUsername());

            redirectAttributes.addFlashAttribute("message", "포트폴리오가 삭제되었습니다.");
            return "redirect:/my-portfolio";

        } catch (Exception e) {
            log.error("❌ 포트폴리오 삭제 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "포트폴리오 삭제 중 오류가 발생했습니다.");
            return "redirect:/my-portfolio";
        }
    }

    /**
     * 공개/비공개 토글
     */
    @PostMapping("/toggle/{id}")
    public String togglePublished(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);
            User currentUser = userService.getUserByUsername(userDetails.getUsername());

            // 권한 체크
            if (!portfolio.isOwnedBy(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "본인의 포트폴리오만 변경할 수 있습니다.");
                return "redirect:/my-portfolio";
            }

            portfolioService.togglePublished(id);
            portfolio = portfolioService.getPortfolioById(id);

            String status = portfolio.isPublished() ? "공개" : "비공개";
            redirectAttributes.addFlashAttribute("message",
                    "포트폴리오가 " + status + " 상태로 변경되었습니다.");

        } catch (Exception e) {
            log.error("❌ 상태 변경 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "상태 변경 중 오류가 발생했습니다.");
        }

        return "redirect:/my-portfolio";
    }

    /**
     * URL 검증 헬퍼 메서드
     */
    private boolean validateUrls(Portfolio portfolio, RedirectAttributes redirectAttributes) {
        if (portfolio.getImageUrl() != null && !portfolio.getImageUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateImageUrl(portfolio.getImageUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error", "이미지 URL 오류: " + result.getMessage());
                return false;
            }
        }

        if (portfolio.getProjectUrl() != null && !portfolio.getProjectUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getProjectUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error", "프로젝트 URL 오류: " + result.getMessage());
                return false;
            }
        }

        if (portfolio.getGithubUrl() != null && !portfolio.getGithubUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getGithubUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error", "GitHub URL 오류: " + result.getMessage());
                return false;
            }
        }

        if (portfolio.getDemoUrl() != null && !portfolio.getDemoUrl().isEmpty()) {
            UrlValidationService.ValidationResult result =
                    urlValidationService.validateUrl(portfolio.getDemoUrl());
            if (!result.isValid()) {
                redirectAttributes.addFlashAttribute("error", "데모 URL 오류: " + result.getMessage());
                return false;
            }
        }

        return true;
    }
}