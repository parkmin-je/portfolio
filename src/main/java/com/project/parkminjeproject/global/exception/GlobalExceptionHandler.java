package com.project.parkminjeproject.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 전역 예외 처리 핸들러
 * - 404, 403, 500 등의 HTTP 에러를 처리
 * - 파일 업로드 에러 처리
 * - 커스텀 예외 처리
 * - 사용자에게 친화적인 에러 메시지 제공
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 Not Found - 페이지를 찾을 수 없음
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model, HttpServletRequest request) {
        log.warn("404 Error - URL: {}, Method: {}",
                request.getRequestURI(), request.getMethod());

        model.addAttribute("status", 404);
        model.addAttribute("error", "페이지를 찾을 수 없습니다");
        model.addAttribute("message", "요청하신 페이지가 존재하지 않습니다.");
        model.addAttribute("path", request.getRequestURI());

        return "error/404";
    }

    /**
     * 404 Not Found - 리소스를 찾을 수 없음 (Spring Boot 3.2+)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model, HttpServletRequest request) {
        log.warn("404 Error - Resource not found: {}", request.getRequestURI());

        model.addAttribute("status", 404);
        model.addAttribute("error", "리소스를 찾을 수 없습니다");
        model.addAttribute("message", "요청하신 리소스가 존재하지 않습니다.");
        model.addAttribute("path", request.getRequestURI());

        return "error/404";
    }

    /**
     * 403 Forbidden - 접근 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        String username = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "Anonymous";

        log.warn("403 Error - User: {}, URL: {}, IP: {}",
                username, request.getRequestURI(), getClientIP(request));

        model.addAttribute("status", 403);
        model.addAttribute("error", "접근 권한이 없습니다");
        model.addAttribute("message", "이 페이지에 접근할 권한이 없습니다.");
        model.addAttribute("path", request.getRequestURI());

        return "error/403";
    }

    /**
     * 413 Payload Too Large - 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, Model model, HttpServletRequest request) {

        log.error("파일 크기 초과 - URL: {}, User: {}",
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous");

        model.addAttribute("status", 413);
        model.addAttribute("error", "파일 크기 초과");
        model.addAttribute("message", "업로드 파일 크기는 5MB를 초과할 수 없습니다.");
        model.addAttribute("path", request.getRequestURI());

        return "error/400";
    }

    /**
     * 커스텀 예외: 포트폴리오를 찾을 수 없음
     */
    @ExceptionHandler(PortfolioNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handlePortfolioNotFound(
            PortfolioNotFoundException ex, Model model, HttpServletRequest request) {

        log.error("Portfolio Not Found - ID: {}, URL: {}",
                ex.getMessage(), request.getRequestURI());

        model.addAttribute("status", 404);
        model.addAttribute("error", "포트폴리오를 찾을 수 없습니다");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());

        return "error/404";
    }

    /**
     * 커스텀 예외: 파일 업로드 실패
     */
    @ExceptionHandler(FileUploadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleFileUploadException(
            FileUploadException ex, Model model, HttpServletRequest request) {

        log.error("File Upload Error - Message: {}, URL: {}",
                ex.getMessage(), request.getRequestURI());

        model.addAttribute("status", 400);
        model.addAttribute("error", "파일 업로드 실패");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());

        return "error/400";
    }

    /**
     * 일반 예외 처리 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralError(
            Exception ex, Model model, HttpServletRequest request) {

        log.error("Unexpected error occurred - URL: {}, User: {}, Error: {}",
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous",
                ex.getMessage(),
                ex);

        model.addAttribute("status", 500);
        model.addAttribute("error", "서버 오류");
        model.addAttribute("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        model.addAttribute("path", request.getRequestURI());

        // 개발 환경에서만 상세 에러 메시지 표시 (선택사항)
        // model.addAttribute("detailMessage", ex.getMessage());

        return "error/500";
    }

    /**
     * 클라이언트 IP 주소 가져오기 (프록시 고려)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
