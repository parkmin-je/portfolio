package com.project.parkminjeproject.dto;

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