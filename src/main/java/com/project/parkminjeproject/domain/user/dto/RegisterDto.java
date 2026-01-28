package com.project.parkminjeproject.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {
    private String username;
    private String password;
    private String passwordConfirm;
    private String name;
    private String email;
}