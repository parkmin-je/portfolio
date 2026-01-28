package com.project.parkminjeproject.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

/**
 * URL 검증 서비스
 * - XSS 공격 방지
 * - 허용된 프로토콜만 사용
 * - 안전한 도메인만 허용 (선택사항)
 */
@Slf4j
@Service
public class UrlValidationService {

    // 허용된 프로토콜
    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("http", "https");

    // 차단할 위험한 패턴
    private static final String[] DANGEROUS_PATTERNS = {
            "javascript:", "data:", "vbscript:", "file:",
            "<script", "</script", "onerror=", "onclick=",
            "onload=", "eval(", "expression("
    };

    /**
     * URL 유효성 검증
     *
     * @param urlString 검증할 URL
     * @return ValidationResult
     */
    public ValidationResult validateUrl(String urlString) {
        // 1. Null/Empty 체크
        if (urlString == null || urlString.trim().isEmpty()) {
            return ValidationResult.success(); // URL은 선택사항
        }

        String url = urlString.trim();

        // 2. 위험한 패턴 체크 (XSS 방지)
        for (String pattern : DANGEROUS_PATTERNS) {
            if (url.toLowerCase().contains(pattern.toLowerCase())) {
                return ValidationResult.failure(
                        "허용되지 않는 URL 패턴이 포함되어 있습니다.");
            }
        }

        // 3. URL 형식 검증
        try {
            URI uri = new URI(url);
            URL parsedUrl = uri.toURL();

            // 4. 프로토콜 검증
            String protocol = parsedUrl.getProtocol().toLowerCase();
            if (!ALLOWED_PROTOCOLS.contains(protocol)) {
                return ValidationResult.failure(
                        "허용되지 않는 프로토콜입니다. (http, https만 가능)");
            }

            // 5. 호스트 검증
            String host = parsedUrl.getHost();
            if (host == null || host.isEmpty()) {
                return ValidationResult.failure("유효하지 않은 URL 형식입니다.");
            }

            // 6. localhost/내부 IP 차단 (선택사항)
            if (isInternalHost(host)) {
                log.warn("내부 호스트 접근 시도: {}", host);
                return ValidationResult.failure(
                        "내부 네트워크 주소는 사용할 수 없습니다.");
            }

            // 7. URL 길이 체크
            if (url.length() > 2000) {
                return ValidationResult.failure("URL이 너무 깁니다. (최대 2000자)");
            }

            return ValidationResult.success();

        } catch (URISyntaxException | MalformedURLException e) {
            log.warn("유효하지 않은 URL: {}", url);
            return ValidationResult.failure(
                    "올바른 URL 형식이 아닙니다. (예: https://example.com/image.jpg)");
        }
    }

    /**
     * 내부 호스트 여부 확인
     * localhost, 127.0.0.1, 사설 IP 대역 차단
     */
    private boolean isInternalHost(String host) {
        host = host.toLowerCase();

        // localhost 변형
        if (host.equals("localhost") || host.equals("127.0.0.1") ||
                host.equals("::1") || host.equals("0.0.0.0")) {
            return true;
        }

        // 사설 IP 대역 (10.x.x.x, 172.16-31.x.x, 192.168.x.x)
        if (host.matches("^10\\..*") ||
                host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") ||
                host.matches("^192\\.168\\..*")) {
            return true;
        }

        return false;
    }

    /**
     * 이미지 URL 검증 (추가 체크)
     */
    public ValidationResult validateImageUrl(String urlString) {
        // 기본 URL 검증
        ValidationResult basicResult = validateUrl(urlString);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        if (urlString == null || urlString.trim().isEmpty()) {
            return ValidationResult.success();
        }

        // 이미지 확장자 체크 (선택사항)
        String url = urlString.toLowerCase();
        boolean hasImageExtension = url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".png") || url.endsWith(".gif") ||
                url.endsWith(".webp") || url.endsWith(".svg");

        // 확장자가 없어도 허용 (CDN URL 등을 위해)
        // 필요시 엄격하게 체크 가능

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
    }
}