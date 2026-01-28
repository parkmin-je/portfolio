package com.project.parkminjeproject.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 감사 로그 엔티티
 * 시스템의 중요 작업을 기록
 */
@Entity
@Getter
@Setter
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // 작업 수행자

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT

    @Column(nullable = false)
    private String entityType; // Portfolio, User 등

    private Long entityId; // 대상 엔티티 ID

    @Column(columnDefinition = "TEXT")
    private String details; // 상세 정보

    private String ipAddress; // IP 주소

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * HTML 템플릿용 - description 필드를 details로 매핑
     */
    public String getDescription() {
        return this.details;
    }
}