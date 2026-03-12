package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sprint_burndown")
public class SprintBurndown {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false, unique = true)
  private Report report;

  @Column(name = "total_stories")
  private Integer totalStories;

  private Integer completed;

  @Column(name = "in_progress")
  private Integer inProgress;

  @Column(name = "remaining_sp")
  private Integer remainingSp;

  @Column(name = "ideal_sp")
  private Integer idealSp;

  private Double variance;

  private String trend;
}
