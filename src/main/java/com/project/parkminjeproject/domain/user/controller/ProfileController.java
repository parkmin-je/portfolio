package com.project.parkminjeproject.domain.user.controller;

import com.project.parkminjeproject.domain.audit.service.AuditLogService;
import com.project.parkminjeproject.domain.user.dto.ProfileUpdateDto;
import com.project.parkminjeproject.domain.user.entity.User;
import com.project.parkminjeproject.domain.user.service.PasswordValidationService;
import com.project.parkminjeproject.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 프로필 컨트롤러 - 비밀번호 검증 및 감사 로그 적용
 */
@Slf4j
@Controller
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final PasswordValidationService passwordValidationService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());

        model.addAttribute("user", user);
        model.addAttribute("profileDto", dto);

        return "admin/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute ProfileUpdateDto profileDto,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(userDetails.getUsername(), profileDto);

            // 감사 로그 기록
            auditLogService.log(
                    "UPDATE",
                    "User",
                    null,
                    "프로필 정보 수정: " + userDetails.getUsername()
            );

            log.info("프로필 수정 - 사용자: {}", userDetails.getUsername());

            redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");
        } catch (Exception e) {
            log.error("프로필 수정 실패 - 사용자: {}, 오류: {}",
                    userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute ProfileUpdateDto profileDto,
                                 RedirectAttributes redirectAttributes,
                                 jakarta.servlet.http.HttpServletRequest request) {
        try {
            String username = userDetails.getUsername();

            // 1. 새 비밀번호 입력 확인
            if (profileDto.getNewPassword() == null ||
                    profileDto.getNewPassword().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호를 입력해주세요.");
                return "redirect:/admin/profile";
            }

            // 2. 비밀번호 일치 확인
            if (!profileDto.getNewPassword().equals(profileDto.getNewPasswordConfirm())) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/admin/profile";
            }

            // 3. 🆕 강력한 비밀번호 검증 (사용자 정보 포함)
            User user = userService.getUserByUsername(username);
            PasswordValidationService.ValidationResult validationResult =
                    passwordValidationService.validatePasswordWithUserInfo(
                            profileDto.getNewPassword(),
                            user.getUsername(),
                            user.getName(),
                            user.getEmail()
                    );

            if (!validationResult.isValid()) {
                redirectAttributes.addFlashAttribute("error", validationResult.getMessage());
                return "redirect:/admin/profile";
            }

            // 4. 비밀번호 변경
            userService.changePassword(
                    username,
                    profileDto.getCurrentPassword(),
                    profileDto.getNewPassword()
            );

            // 5. 감사 로그 기록
            auditLogService.log(
                    "PASSWORD_CHANGE",
                    "User",
                    user.getId(),
                    "비밀번호 변경: " + username
            );

            log.info("비밀번호 변경 성공 - 사용자: {}", username);

            // 6. 🔧 세션 무효화 (자동 로그아웃)
            request.getSession().invalidate();

            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다. 다시 로그인해주세요.");
            return "redirect:/login?passwordChanged=true";
        } catch (Exception e) {
            log.error("비밀번호 변경 실패 - 사용자: {}, 오류: {}",
                    userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/profile";
        }
    }
}