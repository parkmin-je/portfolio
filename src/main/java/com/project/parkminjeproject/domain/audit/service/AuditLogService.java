package com.project.parkminjeproject.domain.audit.service;

import com.project.parkminjeproject.domain.audit.entity.AuditLog;
import com.project.parkminjeproject.domain.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 감사 로그 생성 (디버깅 강화)
     */
    @Transactional
    public void log(String action, String entityType, Long entityId, String details) {
        try {
            log.info("🔵 [AuditLog] 로그 기록 시작 - action: {}, entityType: {}, entityId: {}",
                    action, entityType, entityId);

            AuditLog auditLog = new AuditLog();

            // 현재 사용자 정보
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
            auditLog.setUsername(username);
            log.debug("  └─ username: {}", username);

            // 작업 정보
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            log.debug("  └─ details: {}", details);

            // IP 주소
            String ipAddress = getClientIpAddress();
            auditLog.setIpAddress(ipAddress);
            log.debug("  └─ ipAddress: {}", ipAddress);

            // DB 저장
            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("✅ [AuditLog] 로그 저장 완료 - ID: {}, action: {}, username: {}",
                    saved.getId(), saved.getAction(), saved.getUsername());

            // 저장 확인
            long totalCount = auditLogRepository.count();
            log.info("  └─ 현재 전체 로그 개수: {}", totalCount);

        } catch (Exception e) {
            log.error("❌ [AuditLog] 로그 저장 실패: {}", e.getMessage(), e);
            // 예외를 다시 던지지 않음 - 로그 실패가 메인 기능에 영향을 주지 않도록
        }
    }

    /**
     * 포트폴리오 생성 로그
     */
    public void logPortfolioCreated(Long portfolioId, String title) {
        log.info("📝 [AuditLog] 포트폴리오 생성 로그 호출 - ID: {}, 제목: {}", portfolioId, title);
        log("CREATE", "Portfolio", portfolioId, "포트폴리오 생성: " + title);
    }

    /**
     * 포트폴리오 수정 로그
     */
    public void logPortfolioUpdated(Long portfolioId, String title) {
        log.info("✏️ [AuditLog] 포트폴리오 수정 로그 호출 - ID: {}, 제목: {}", portfolioId, title);
        log("UPDATE", "Portfolio", portfolioId, "포트폴리오 수정: " + title);
    }

    /**
     * 포트폴리오 삭제 로그
     */
    public void logPortfolioDeleted(Long portfolioId, String title) {
        log.info("🗑️ [AuditLog] 포트폴리오 삭제 로그 호출 - ID: {}, 제목: {}", portfolioId, title);
        log("DELETE", "Portfolio", portfolioId, "포트폴리오 삭제: " + title);
    }

    /**
     * 로그인 로그
     */
    public void logLogin(String username) {
        log.info("🔐 [AuditLog] 로그인 로그 호출 - username: {}", username);

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setAction("LOGIN");
            auditLog.setEntityType("User");
            auditLog.setDetails("사용자 로그인");
            auditLog.setIpAddress(getClientIpAddress());

            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("✅ [AuditLog] 로그인 로그 저장 완료 - ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("❌ [AuditLog] 로그인 로그 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 클라이언트 IP 주소 조회
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            log.warn("IP 주소 추출 실패: {}", e.getMessage());
            return "unknown";
        }
    }
}