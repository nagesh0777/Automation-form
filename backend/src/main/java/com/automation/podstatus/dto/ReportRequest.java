package com.automation.podstatus.dto;

import com.automation.podstatus.domain.BlockerType;
import com.automation.podstatus.domain.PipelineStatus;
import com.automation.podstatus.domain.WorkItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ReportRequest {
  @NotBlank
  private String podName;

  @NotNull
  private LocalDate date;

  @NotNull
  private Integer sprintNumber;

  @NotNull
  private Integer dayOfSprint;

  private String goal;
  private String scrumMasterName;
  private String managerName;

  private SprintBurndownDto sprintBurndown;
  private DoraMetricsDto doraMetrics;
  private FocusOutlookDto focusOutlook;

  private List<PipelineTrackerDto> pipelineTrackers = new ArrayList<>();
  private List<WorkItemDto> workItems = new ArrayList<>();
  private List<BlockerDependencyDto> blockersDependencies = new ArrayList<>();
  private List<RiskObservationDto> risksObservations = new ArrayList<>();

  @Data
  public static class SprintBurndownDto {
    private Integer totalStories;
    private Integer completed;
    private Integer inProgress;
    private Integer remainingSp;
    private Integer idealSp;
    private Double variance;
    private String trend;
  }

  @Data
  public static class DoraMetricsDto {
    private Double deploymentFrequencyToday;
    private Double deploymentFrequencySprintAvg;
    private Double deploymentFrequencyTarget;
    private Double leadTimeToday;
    private Double leadTimeAvg;
    private Double leadTimeTarget;
    private Double changeFailureRateToday;
    private Double changeFailureRateAvg;
    private Double changeFailureRateTarget;
    private Double mttrToday;
    private Double mttrAvg;
    private Double mttrTarget;
  }

  @Data
  public static class PipelineTrackerDto {
    private String itemId;
    private PipelineStatus dataPrep;
    private PipelineStatus modelDev;
    private PipelineStatus integration;
    private PipelineStatus validation;
    private PipelineStatus deployment;
    private String notes;
  }

  @Data
  public static class WorkItemDto {
    private String memberName;
    private String storyId;
    private String taskDescription;
    private WorkItemStatus status;
    private Integer ageDays;
    private String flag;
  }

  @Data
  public static class BlockerDependencyDto {
    private String itemId;
    private BlockerType type;
    private String description;
    private String owner;
    private String actionNeeded;
  }

  @Data
  public static class RiskObservationDto {
    private String description;
  }

  @Data
  public static class FocusOutlookDto {
    private String todayFocus;
    private String tomorrowOutlook;
    private String decisionsNeeded;
  }
}
