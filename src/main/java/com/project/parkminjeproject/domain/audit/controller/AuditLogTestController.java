package com.project.parkminjeproject.domain.audit.controller;

import com.project.parkminjeproject.domain.audit.entity.AuditLog;
import com.project.parkminjeproject.domain.audit.repository.AuditLogRepository;
import com.project.parkminjeproject.domain.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AuditLog 테스트 및 진단용 컨트롤러
 *
 * 접속: http://localhost:8080/admin/test/audit
 */
@Slf4j
@Controller
@RequestMapping("/admin/test")
@RequiredArgsConstructor
public class AuditLogTestController {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    /**
     * 테스트 로그 생성 및 진단
     * 접속: http://localhost:8080/admin/test/audit
     */
    @GetMapping("/audit")
    @ResponseBody
    public Map<String, Object> testAuditLog() {
        Map<String, Object> result = new HashMap<>();

        log.info("========================================");
        log.info("🔍 AuditLog 진단 시작");
        log.info("========================================");

        try {
            // 1. 저장 전 개수
            long beforeCount = auditLogRepository.count();
            log.info("1️⃣ 저장 전 로그 개수: {}", beforeCount);
            result.put("beforeCount", beforeCount);

            // 2. 테스트 로그 생성
            log.info("2️⃣ 테스트 로그 생성 시도...");
            auditLogService.log("TEST", "TestEntity", 999L, "테스트 로그 생성: " + LocalDateTime.now());

            // 3. 저장 후 개수
            long afterCount = auditLogRepository.count();
            log.info("3️⃣ 저장 후 로그 개수: {}", afterCount);
            result.put("afterCount", afterCount);

            // 4. 증가 여부 확인
            boolean increased = afterCount > beforeCount;
            log.info("4️⃣ 로그 증가 여부: {}", increased ? "✅ 성공" : "❌ 실패");
            result.put("success", increased);

            // 5. 최근 로그 5개 조회
            List<AuditLog> recentLogs = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
            log.info("5️⃣ 최근 로그 {}개 조회", recentLogs.size());
            result.put("recentLogsCount", recentLogs.size());

            // 6. 최근 로그 상세
            if (!recentLogs.isEmpty()) {
                AuditLog latest = recentLogs.get(0);
                log.info("   📋 최신 로그:");
                log.info("      - ID: {}", latest.getId());
                log.info("      - Username: {}", latest.getUsername());
                log.info("      - Action: {}", latest.getAction());
                log.info("      - EntityType: {}", latest.getEntityType());
                log.info("      - Details: {}", latest.getDetails());
                log.info("      - CreatedAt: {}", latest.getCreatedAt());

                Map<String, Object> latestLog = new HashMap<>();
                latestLog.put("id", latest.getId());
                latestLog.put("username", latest.getUsername());
                latestLog.put("action", latest.getAction());
                latestLog.put("entityType", latest.getEntityType());
                latestLog.put("details", latest.getDetails());
                latestLog.put("createdAt", latest.getCreatedAt());
                result.put("latestLog", latestLog);
            }

            // 7. 데이터베이스 연결 확인
            result.put("databaseConnected", true);
            result.put("repositoryWorking", true);

            log.info("========================================");
            log.info("✅ AuditLog 진단 완료");
            log.info("========================================");

            result.put("status", "SUCCESS");
            result.put("message", "AuditLog 시스템이 정상 작동합니다.");

        } catch (Exception e) {
            log.error("========================================");
            log.error("❌ AuditLog 진단 실패: {}", e.getMessage(), e);
            log.error("========================================");

            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            result.put("stackTrace", e.getStackTrace());
        }

        return result;
    }

    /**
     * 대량 테스트 로그 생성
     * 접속: http://localhost:8080/admin/test/audit/bulk?count=10
     */
    @GetMapping("/audit/bulk")
    @ResponseBody
    public Map<String, Object> createBulkTestLogs(Integer count) {
        Map<String, Object> result = new HashMap<>();

        if (count == null || count <= 0) {
            count = 5;
        }

        if (count > 100) {
            count = 100; // 최대 100개
        }

        log.info("🔄 대량 테스트 로그 생성 시작 - 개수: {}", count);

        long beforeCount = auditLogRepository.count();

        for (int i = 1; i <= count; i++) {
            try {
                auditLogService.log(
                        "TEST_" + i,
                        "TestEntity",
                        (long) i,
                        String.format("테스트 로그 #%d - %s", i, LocalDateTime.now())
                );
            } catch (Exception e) {
                log.error("테스트 로그 {} 생성 실패: {}", i, e.getMessage());
            }
        }

        long afterCount = auditLogRepository.count();
        long created = afterCount - beforeCount;

        log.info("✅ 대량 테스트 로그 생성 완료 - 생성: {}/{}", created, count);

        result.put("status", "SUCCESS");
        result.put("requestedCount", count);
        result.put("createdCount", created);
        result.put("beforeCount", beforeCount);
        result.put("afterCount", afterCount);

        return result;
    }

    /**
     * 로그 통계 조회
     * 접속: http://localhost:8080/admin/test/audit/stats
     */
    @GetMapping("/audit/stats")
    @ResponseBody
    public Map<String, Object> getAuditLogStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalCount = auditLogRepository.count();
            List<AuditLog> recent = auditLogRepository.findTop10ByOrderByCreatedAtDesc();

            stats.put("totalLogs", totalCount);
            stats.put("recentLogsCount", recent.size());

            if (!recent.isEmpty()) {
                stats.put("latestLogDate", recent.get(0).getCreatedAt());
                stats.put("oldestRecentLogDate", recent.get(recent.size() - 1).getCreatedAt());
            }

            stats.put("status", "SUCCESS");

        } catch (Exception e) {
            stats.put("status", "ERROR");
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}