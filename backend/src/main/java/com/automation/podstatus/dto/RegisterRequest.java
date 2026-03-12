package com.automation.podstatus.dto;

import com.automation.podstatus.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
  @NotBlank private String name;
  @Email @NotBlank private String email;
  @NotBlank private String password;
  private Role role;
}
