package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "pipeline_tracker")
public class PipelineTracker {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(name = "item_id", nullable = false)
  private String itemId;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_prep")
  private PipelineStatus dataPrep;

  @Enumerated(EnumType.STRING)
  @Column(name = "model_dev")
  private PipelineStatus modelDev;

  @Enumerated(EnumType.STRING)
  private PipelineStatus integration;

  @Enumerated(EnumType.STRING)
  private PipelineStatus validation;

  @Enumerated(EnumType.STRING)
  private PipelineStatus deployment;

  @Column(columnDefinition = "TEXT")
  private String notes;
}
