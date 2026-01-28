package com.project.parkminjeproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER"; // 기본값을 ROLE_USER로 변경

    @Column(nullable = false)
    private boolean enabled = true;

    // ✅ 추가: 사용자 프로필 정보
    @Column(length = 500)
    private String bio; // 자기소개

    @Column(length = 255)
    private String profileImageUrl; // 프로필 이미지

    @Column(length = 100)
    private String company; // 회사/소속

    @Column(length = 100)
    private String position; // 직책

    @Column(length = 255)
    private String website; // 개인 웹사이트

    @Column(length = 255)
    private String github; // GitHub URL

    @Column(length = 255)
    private String linkedin; // LinkedIn URL

    // ✅ 추가: 포트폴리오 관계 (양방향)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Portfolio> portfolios = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ 헬퍼 메서드
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }

    public boolean isUser() {
        return "ROLE_USER".equals(this.role);
    }
}