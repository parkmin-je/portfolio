package com.project.parkminjeproject.domain.user.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * 비밀번호 강도 검증 서비스
 * - 최소 8자 이상
 * - 대문자, 소문자, 숫자, 특수문자 각 1개 이상 포함
 * - 일반적인 비밀번호 체크
 */
@Service
public class PasswordValidationService {

    // 최소 8자, 대문자 1개, 소문자 1개, 숫자 1개, 특수문자 1개
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // 일반적인 비밀번호 패턴 (금지)
    private static final String[] COMMON_PASSWORDS = {
            "password", "Password1!", "admin", "Admin123!",
            "12345678", "qwerty", "letmein", "welcome"
    };

    /**
     * 비밀번호 강도 검증
     *
     * @param password 검증할 비밀번호
     * @return ValidationResult 검증 결과
     */
    public ValidationResult validatePassword(String password) {
        // 1. Null/Empty 체크
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure("비밀번호를 입력해주세요.");
        }

        // 2. 길이 체크 (최소 8자)
        if (password.length() < 8) {
            return ValidationResult.failure("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 3. 최대 길이 체크 (100자)
        if (password.length() > 100) {
            return ValidationResult.failure("비밀번호는 100자를 초과할 수 없습니다.");
        }

        // 4. 패턴 검증 (대소문자, 숫자, 특수문자)
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.failure(
                    "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 각각 1개 이상 포함해야 합니다."
            );
        }

        // 5. 일반적인 비밀번호 체크
        String lowerPassword = password.toLowerCase();
        for (String commonPassword : COMMON_PASSWORDS) {
            if (lowerPassword.contains(commonPassword.toLowerCase())) {
                return ValidationResult.failure(
                        "너무 흔한 비밀번호입니다. 다른 비밀번호를 사용해주세요."
                );
            }
        }

        // 6. 연속된 문자/숫자 체크 (선택사항)
        if (hasSequentialCharacters(password)) {
            return ValidationResult.failure(
                    "연속된 문자나 숫자는 사용할 수 없습니다. (예: abc, 123)"
            );
        }

        // 7. 반복 문자 체크
        if (hasRepeatingCharacters(password)) {
            return ValidationResult.failure(
                    "동일한 문자가 3번 이상 반복됩니다. 다른 비밀번호를 사용해주세요."
            );
        }

        return ValidationResult.success();
    }

    /**
     * 연속된 문자/숫자 체크 (abc, 123 등)
     */
    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char first = password.charAt(i);
            char second = password.charAt(i + 1);
            char third = password.charAt(i + 2);

            // 연속된 숫자 체크 (123, 234 등)
            if (Character.isDigit(first) && Character.isDigit(second) && Character.isDigit(third)) {
                if (second == first + 1 && third == second + 1) {
                    return true;
                }
                // 역순 체크 (321, 432 등)
                if (second == first - 1 && third == second - 1) {
                    return true;
                }
            }

            // 연속된 알파벳 체크 (abc, xyz 등)
            if (Character.isLetter(first) && Character.isLetter(second) && Character.isLetter(third)) {
                if (second == first + 1 && third == second + 1) {
                    return true;
                }
                // 역순 체크 (cba, zyx 등)
                if (second == first - 1 && third == second - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 반복 문자 체크 (aaa, 111 등)
     */
    private boolean hasRepeatingCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                    password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 사용자 정보 기반 검증 (선택사항)
     * - 비밀번호에 아이디, 이름, 이메일이 포함되어 있는지 확인
     *
     * @param password 비밀번호
     * @param username 사용자 아이디
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @return ValidationResult
     */
    public ValidationResult validatePasswordWithUserInfo(
            String password, String username, String name, String email) {

        // 기본 비밀번호 검증
        ValidationResult basicResult = validatePassword(password);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        String lowerPassword = password.toLowerCase();

        // 아이디가 포함되어 있는지 확인
        if (username != null && username.length() >= 3) {
            if (lowerPassword.contains(username.toLowerCase())) {
                return ValidationResult.failure(
                        "비밀번호에 아이디가 포함될 수 없습니다."
                );
            }
        }

        // 이름이 포함되어 있는지 확인
        if (name != null && name.length() >= 2) {
            if (lowerPassword.contains(name.toLowerCase())) {
                return ValidationResult.failure(
                        "비밀번호에 이름이 포함될 수 없습니다."
                );
            }
        }

        // 이메일 로컬 파트가 포함되어 있는지 확인
        if (email != null && email.contains("@")) {
            String emailLocal = email.substring(0, email.indexOf("@")).toLowerCase();
            if (emailLocal.length() >= 3 && lowerPassword.contains(emailLocal)) {
                return ValidationResult.failure(
                        "비밀번호에 이메일 주소가 포함될 수 없습니다."
                );
            }
        }

        return ValidationResult.success();
    }

    /**
     * 검증 결과 클래스
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + message;
        }
    }
}
