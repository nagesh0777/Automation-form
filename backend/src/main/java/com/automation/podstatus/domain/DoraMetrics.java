package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dora_metrics")
public class DoraMetrics {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false, unique = true)
  private Report report;

  @Column(name = "deployment_frequency_today")
  private Double deploymentFrequencyToday;

  @Column(name = "deployment_frequency_sprint_avg")
  private Double deploymentFrequencySprintAvg;

  @Column(name = "deployment_frequency_target")
  private Double deploymentFrequencyTarget;

  @Column(name = "lead_time_today")
  private Double leadTimeToday;

  @Column(name = "lead_time_avg")
  private Double leadTimeAvg;

  @Column(name = "lead_time_target")
  private Double leadTimeTarget;

  @Column(name = "change_failure_rate_today")
  private Double changeFailureRateToday;

  @Column(name = "change_failure_rate_avg")
  private Double changeFailureRateAvg;

  @Column(name = "change_failure_rate_target")
  private Double changeFailureRateTarget;

  @Column(name = "mttr_today")
  private Double mttrToday;

  @Column(name = "mttr_avg")
  private Double mttrAvg;

  @Column(name = "mttr_target")
  private Double mttrTarget;
}
