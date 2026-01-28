package com.project.parkminjeproject.domain.admin.controller;

import com.project.parkminjeproject.domain.user.entity.User;
import com.project.parkminjeproject.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 - 사용자 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * 사용자 목록 조회
     */
    @GetMapping
    public String userList(Model model) {
        log.info("모든 사용자 조회");

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        return "admin/user-list";
    }

    /**
     * 사용자 권한 변경
     */
    @PostMapping("/{userId}/role")
    public String updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        log.info("사용자 권한 변경 - userId: {}, newRole: {}", userId, role);

        try {
            // 권한 검증
            if (!role.equals("ROLE_USER") && !role.equals("ROLE_ADMIN")) {
                log.warn("잘못된 권한 값: {}", role);
                redirectAttributes.addFlashAttribute("error", "잘못된 권한 값입니다.");
                return "redirect:/admin/users";
            }

            userService.updateUserRole(userId, role);

            log.info("✅ 사용자 권한 변경 완료 - userId: {}, newRole: {}", userId, role);
            redirectAttributes.addFlashAttribute("message", "사용자 권한이 변경되었습니다.");

        } catch (Exception e) {
            log.error("❌ 권한 변경 실패 - userId: {}, error: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "권한 변경에 실패했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * 사용자 활성화/비활성화
     */
    @PostMapping("/{userId}/status")
    public String updateUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes) {

        log.info("사용자 상태 변경 - userId: {}, enabled: {}", userId, enabled);

        try {
            userService.updateUserEnabled(userId, enabled);

            String status = enabled ? "활성화" : "비활성화";
            log.info("✅ 사용자 상태 변경 완료 - userId: {}, status: {}", userId, status);
            redirectAttributes.addFlashAttribute("message", "사용자가 " + status + "되었습니다.");

        } catch (Exception e) {
            log.error("❌ 상태 변경 실패 - userId: {}, error: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "상태 변경에 실패했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * 사용자 삭제
     */
    @PostMapping("/{userId}/delete")
    public String deleteUser(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("사용자 삭제 시도 - userId: {}", userId);

        try {
            // 현재 로그인한 사용자는 삭제 불가 (선택사항)
            // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // if (auth != null && auth.getName().equals(userService.getUserById(userId).getUsername())) {
            //     redirectAttributes.addFlashAttribute("error", "본인 계정은 삭제할 수 없습니다.");
            //     return "redirect:/admin/users";
            // }

            userService.deleteUser(userId);

            log.info("✅ 사용자 삭제 완료 - userId: {}", userId);
            redirectAttributes.addFlashAttribute("message", "사용자가 삭제되었습니다.");

        } catch (Exception e) {
            log.error("❌ 사용자 삭제 실패 - userId: {}, error: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "사용자 삭제에 실패했습니다: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * 사용자 상세 조회 (선택사항)
     */
    @GetMapping("/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        log.info("사용자 상세 조회 - userId: {}", userId);

        try {
            User user = userService.getUserById(userId);
            model.addAttribute("user", user);
            return "admin/user-detail";

        } catch (Exception e) {
            log.error("❌ 사용자 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return "redirect:/admin/users";
        }
    }
}