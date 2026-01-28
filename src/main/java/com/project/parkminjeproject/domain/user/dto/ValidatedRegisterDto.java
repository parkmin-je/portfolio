package com.project.parkminjeproject.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO (유효성 검증 추가)
 */
@Getter
@Setter
public class ValidatedRegisterDto {

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 사용 가능합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 4, max = 20, message = "비밀번호는 4-20자 사이여야 합니다")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}

/**
 * 포트폴리오 생성/수정 DTO (유효성 검증 추가)
 */
@Getter
@Setter
class PortfolioDto {

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 2, max = 200, message = "제목은 2-200자 사이여야 합니다")
    private String title;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @NotBlank(message = "설명은 필수입니다")
    @Size(min = 10, max = 5000, message = "설명은 10-5000자 사이여야 합니다")
    private String description;

    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다")
    private String imageUrl;

    @Size(max = 500, message = "프로젝트 URL은 500자를 초과할 수 없습니다")
    private String projectUrl;

    private boolean published = true;
}

/**
 * 프로필 수정 DTO (유효성 검증 추가)
 */
@Getter
@Setter
class ValidatedProfileUpdateDto {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // 비밀번호 변경 시에만 사용
    private String currentPassword;

    @Size(min = 4, max = 20, message = "비밀번호는 4-20자 사이여야 합니다")
    private String newPassword;

    private String newPasswordConfirm;
}