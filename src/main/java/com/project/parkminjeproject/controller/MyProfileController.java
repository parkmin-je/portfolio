package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.audit.AuditLogService;
import com.project.parkminjeproject.dto.ProfileUpdateDto;
import com.project.parkminjeproject.entity.User;
import com.project.parkminjeproject.service.PasswordValidationService;
import com.project.parkminjeproject.service.UserService;
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
 * 일반 사용자용 프로필 설정 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/my-profile")
@RequiredArgsConstructor
public class MyProfileController {

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

        return "my-profile/settings";
    }

    /**
     * 이메일 수정
     */
    @PostMapping("/update-email")
    public String updateEmail(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute ProfileUpdateDto profileDto,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = userDetails.getUsername();
            User user = userService.getUserByUsername(username);

            // 이메일 변경
            if (profileDto.getEmail() != null && !profileDto.getEmail().isEmpty()) {
                ProfileUpdateDto updateDto = new ProfileUpdateDto();
                updateDto.setName(user.getName()); // 이름 유지
                updateDto.setEmail(profileDto.getEmail()); // 이메일만 변경

                userService.updateProfile(username, updateDto);

                auditLogService.log(
                        "UPDATE",
                        "User",
                        user.getId(),
                        "이메일 변경: " + username + " → " + profileDto.getEmail()
                );

                log.info("✅ 이메일 변경 - 사용자: {}, 새 이메일: {}",
                        username, profileDto.getEmail());

                redirectAttributes.addFlashAttribute("message", "이메일이 변경되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("error", "이메일을 입력해주세요.");
            }
        } catch (Exception e) {
            log.error("❌ 이메일 변경 실패 - 사용자: {}, 오류: {}",
                    userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "이메일 변경 중 오류가 발생했습니다.");
        }

        return "redirect:/my-profile";
    }

    /**
     * 비밀번호 변경
     */
    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute ProfileUpdateDto profileDto,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = userDetails.getUsername();

            // 1. 현재 비밀번호 입력 확인
            if (profileDto.getCurrentPassword() == null ||
                    profileDto.getCurrentPassword().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "현재 비밀번호를 입력해주세요.");
                return "redirect:/my-profile";
            }

            // 2. 새 비밀번호 입력 확인
            if (profileDto.getNewPassword() == null ||
                    profileDto.getNewPassword().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호를 입력해주세요.");
                return "redirect:/my-profile";
            }

            // 3. 비밀번호 일치 확인
            if (!profileDto.getNewPassword().equals(profileDto.getNewPasswordConfirm())) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/my-profile";
            }

            // 4. 강력한 비밀번호 검증
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
                return "redirect:/my-profile";
            }

            // 5. 비밀번호 변경
            userService.changePassword(
                    username,
                    profileDto.getCurrentPassword(),
                    profileDto.getNewPassword()
            );

            // 6. 감사 로그 기록
            auditLogService.log(
                    "PASSWORD_CHANGE",
                    "User",
                    user.getId(),
                    "비밀번호 변경: " + username
            );

            log.info("✅ 비밀번호 변경 성공 - 사용자: {}", username);

            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } catch (Exception e) {
            log.error("❌ 비밀번호 변경 실패 - 사용자: {}, 오류: {}",
                    userDetails.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-profile";
    }
}