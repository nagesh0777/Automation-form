package com.automation.podstatus.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportResponse {
  private Long id;
  private String podName;
  private LocalDate date;
  private Integer sprintNumber;
  private Integer dayOfSprint;
  private String goal;
  private String scrumMasterName;
  private String managerName;
  private Integer versionNumber;
  private String warningText;
  private String createdBy;
  private Instant createdAt;

  private ReportRequest.SprintBurndownDto sprintBurndown;
  private ReportRequest.DoraMetricsDto doraMetrics;
  private ReportRequest.FocusOutlookDto focusOutlook;
  private List<ReportRequest.PipelineTrackerDto> pipelineTrackers;
  private List<ReportRequest.WorkItemDto> workItems;
  private List<ReportRequest.BlockerDependencyDto> blockersDependencies;
  private List<ReportRequest.RiskObservationDto> risksObservations;
}
