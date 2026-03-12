package com.automation.podstatus.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(name = "export_history")
public class ExportHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(nullable = false)
  private String format;

  @Column(name = "generated_by", nullable = false)
  private String generatedBy;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  @PrePersist
  void prePersist() {
    if (generatedAt == null) {
      generatedAt = Instant.now();
    }
  }
}
