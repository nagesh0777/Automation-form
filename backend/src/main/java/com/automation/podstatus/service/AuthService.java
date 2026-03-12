package com.automation.podstatus.service;

import com.automation.podstatus.domain.Role;
import com.automation.podstatus.dto.AuthResponse;
import com.automation.podstatus.dto.LoginRequest;
import com.automation.podstatus.dto.RegisterRequest;
import com.automation.podstatus.repository.UserRepository;
import com.automation.podstatus.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository,
                     PasswordEncoder passwordEncoder,
                     AuthenticationManager authenticationManager,
                     JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
}
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }

    com.automation.podstatus.domain.User user = new com.automation.podstatus.domain.User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setRole(request.getRole() == null ? Role.TEAM_MEMBER : request.getRole());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    userRepository.save(user);

    org.springframework.security.core.userdetails.User springUser = new User(
        user.getEmail(), user.getPassword(),
        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );

    return new AuthResponse(jwtService.generateToken(springUser), user.getEmail(), user.getName(), user.getRole());
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );

    com.automation.podstatus.domain.User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    org.springframework.security.core.userdetails.User springUser = new User(
        user.getEmail(), user.getPassword(),
        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );

    return new AuthResponse(jwtService.generateToken(springUser), user.getEmail(), user.getName(), user.getRole());
  }
}
