package com.project.parkminjeproject.domain.category.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 카테고리 엔티티
 * 포트폴리오 카테고리를 동적으로 관리
 */
@Entity
@Getter
@Setter
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 카테고리 이름

    @Column(length = 200)
    private String description; // 카테고리 설명

    @Column(length = 50)
    private String icon; // 아이콘 (이모지 또는 클래스명)

    @Column(nullable = false)
    private Integer displayOrder = 0; // 표시 순서

    @Column(nullable = false)
    private boolean active = true; // 활성화 여부

    private LocalDateTime createdAt;
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
}