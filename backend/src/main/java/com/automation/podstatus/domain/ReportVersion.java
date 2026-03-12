package com.automation.podstatus.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(name = "report_versions")
public class ReportVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
