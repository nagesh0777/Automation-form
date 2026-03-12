package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "risks_observations")
public class RiskObservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(columnDefinition = "TEXT")
  private String description;
}
