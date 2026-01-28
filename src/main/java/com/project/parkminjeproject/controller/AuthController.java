package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.audit.AuditLogService;
import com.project.parkminjeproject.dto.RegisterDto;
import com.project.parkminjeproject.entity.User;
import com.project.parkminjeproject.service.PasswordValidationService;
import com.project.parkminjeproject.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 인증 컨트롤러 - 감사 로그 및 비밀번호 검증 적용
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final PasswordValidationService passwordValidationService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {

        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDto registerDto,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        try {
            // 1. 비밀번호 확인
            if (!registerDto.getPassword().equals(registerDto.getPasswordConfirm())) {
                redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
                return "redirect:/register";
            }

            // 2. 아이디 중복 체크
            if (userService.isUsernameTaken(registerDto.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "이미 사용 중인 아이디입니다.");
                return "redirect:/register";
            }

            // 3. 강력한 비밀번호 검증 (사용자 정보 포함)
            PasswordValidationService.ValidationResult validationResult =
                    passwordValidationService.validatePasswordWithUserInfo(
                            registerDto.getPassword(),
                            registerDto.getUsername(),
                            registerDto.getName(),
                            registerDto.getEmail()
                    );

            if (!validationResult.isValid()) {
                redirectAttributes.addFlashAttribute("error", validationResult.getMessage());
                return "redirect:/register";
            }

            // 4. User 엔티티 생성
            User user = new User();
            user.setUsername(registerDto.getUsername());
            user.setPassword(registerDto.getPassword());
            user.setName(registerDto.getName());
            user.setEmail(registerDto.getEmail());

            // 5. 회원가입
            userService.registerUser(user);

            // 6. 감사 로그 기록
            auditLogService.log(
                    "REGISTER",
                    "User",
                    user.getId(),
                    "회원가입: " + user.getUsername()
            );

            log.info("회원가입 성공 - 사용자: {}, IP: {}",
                    user.getUsername(), getClientIP(request));

            redirectAttributes.addFlashAttribute("message",
                    "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "회원가입 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/register";
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}