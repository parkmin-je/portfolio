package com.project.parkminjeproject.domain.portfolio.entity;

import com.project.parkminjeproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 추가: 작성자 (필수)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String projectUrl;

    @Column(length = 500)
    private String githubUrl;

    @Column(length = 500)
    private String demoUrl;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String technologies;

    @Column(length = 100)
    private String duration;

    @Column(length = 200)
    private String role;

    @Column(length = 200)
    private String client;

    @Column(length = 100)
    private String teamSize;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Column(columnDefinition = "TEXT")
    private String challenges;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(nullable = false)
    private boolean published = true;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String[] getTechnologyArray() {
        if (technologies == null || technologies.isEmpty()) {
            return new String[0];
        }
        return technologies.split(",");
    }

    public void setTechnologyArray(String[] techArray) {
        if (techArray == null || techArray.length == 0) {
            this.technologies = "";
        } else {
            this.technologies = String.join(",", techArray);
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // ✅ 헬퍼 메서드: 소유자 확인
    public boolean isOwnedBy(User user) {
        if (user == null || this.user == null) {
            return false;
        }
        return this.user.getId().equals(user.getId());
    }

    public boolean isOwnedBy(Long userId) {
        if (userId == null || this.user == null) {
            return false;
        }
        return this.user.getId().equals(userId);
    }
}