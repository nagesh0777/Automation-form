package com.automation.podstatus.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "reports")
public class Report {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pod_name", nullable = false)
  private String podName;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "sprint_number", nullable = false)
  private Integer sprintNumber;

  @Column(name = "day_of_sprint", nullable = false)
  private Integer dayOfSprint;

  @Column(columnDefinition = "TEXT")
  private String goal;

  @Column(name = "scrum_master_name")
  private String scrumMasterName;

  @Column(name = "manager_name")
  private String managerName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber = 1;

  @Column(name = "warning_text", columnDefinition = "TEXT")
  private String warningText;

  @OneToOne(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private SprintBurndown sprintBurndown;

  @OneToOne(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private DoraMetrics doraMetrics;

  @OneToOne(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private FocusOutlook focusOutlook;

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PipelineTracker> pipelineTrackers = new ArrayList<>();

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkItem> workItems = new ArrayList<>();

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BlockerDependency> blockersDependencies = new ArrayList<>();

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RiskObservation> risksObservations = new ArrayList<>();

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReportVersion> versions = new ArrayList<>();

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExportHistory> exportHistory = new ArrayList<>();

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
