package com.project.parkminjeproject.domain.audit.repository;

import com.project.parkminjeproject.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 사용자별 로그 조회
    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    // 액션별 로그 조회
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    // 기간별 로그 조회
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 최근 로그 조회
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
}