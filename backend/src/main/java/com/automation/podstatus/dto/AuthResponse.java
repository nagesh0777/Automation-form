package com.automation.podstatus.dto;

import com.automation.podstatus.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
  private String token;
  private String email;
  private String name;
  private Role role;
}
