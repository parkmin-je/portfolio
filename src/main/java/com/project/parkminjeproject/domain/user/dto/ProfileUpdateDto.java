package com.project.parkminjeproject.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateDto {
    private String name;
    private String email;
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
}