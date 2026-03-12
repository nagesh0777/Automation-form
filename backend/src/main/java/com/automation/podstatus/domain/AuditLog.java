package com.automation.podstatus.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_email", nullable = false)
  private String userEmail;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @Column(name = "entity_id")
  private String entityId;

  @Column(columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
