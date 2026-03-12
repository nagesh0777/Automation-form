package com.automation.podstatus.service;

import com.automation.podstatus.domain.*;
import com.automation.podstatus.dto.ReportRequest;
import com.automation.podstatus.dto.ReportResponse;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

  public void updateEntity(Report report, ReportRequest request) {
    report.setPodName(request.getPodName());
    report.setDate(request.getDate());
    report.setSprintNumber(request.getSprintNumber());
    report.setDayOfSprint(request.getDayOfSprint());
    report.setGoal(request.getGoal());
    report.setScrumMasterName(request.getScrumMasterName());
    report.setManagerName(request.getManagerName());

    applySprintBurndown(report, request.getSprintBurndown());
    applyDora(report, request.getDoraMetrics());
    applyFocus(report, request.getFocusOutlook());
    applyPipeline(report, request);
    applyWorkItems(report, request);
    applyBlockers(report, request);
    applyRisks(report, request);
  }

  private void applySprintBurndown(Report report, ReportRequest.SprintBurndownDto dto) {
    if (dto == null) return;
    SprintBurndown s = report.getSprintBurndown() == null ? new SprintBurndown() : report.getSprintBurndown();
    s.setReport(report);
    s.setTotalStories(dto.getTotalStories());
    s.setCompleted(dto.getCompleted());
    s.setInProgress(dto.getInProgress());
    s.setRemainingSp(dto.getRemainingSp());
    s.setIdealSp(dto.getIdealSp());
    s.setVariance(dto.getVariance());
    s.setTrend(dto.getTrend());
    report.setSprintBurndown(s);
  }

  private void applyDora(Report report, ReportRequest.DoraMetricsDto dto) {
    if (dto == null) return;
    DoraMetrics d = report.getDoraMetrics() == null ? new DoraMetrics() : report.getDoraMetrics();
    d.setReport(report);
    d.setDeploymentFrequencyToday(dto.getDeploymentFrequencyToday());
    d.setDeploymentFrequencySprintAvg(dto.getDeploymentFrequencySprintAvg());
    d.setDeploymentFrequencyTarget(dto.getDeploymentFrequencyTarget());
    d.setLeadTimeToday(dto.getLeadTimeToday());
    d.setLeadTimeAvg(dto.getLeadTimeAvg());
    d.setLeadTimeTarget(dto.getLeadTimeTarget());
    d.setChangeFailureRateToday(dto.getChangeFailureRateToday());
    d.setChangeFailureRateAvg(dto.getChangeFailureRateAvg());
    d.setChangeFailureRateTarget(dto.getChangeFailureRateTarget());
    d.setMttrToday(dto.getMttrToday());
    d.setMttrAvg(dto.getMttrAvg());
    d.setMttrTarget(dto.getMttrTarget());
    report.setDoraMetrics(d);
  }

  private void applyFocus(Report report, ReportRequest.FocusOutlookDto dto) {
    if (dto == null) return;
    FocusOutlook f = report.getFocusOutlook() == null ? new FocusOutlook() : report.getFocusOutlook();
    f.setReport(report);
    f.setTodayFocus(dto.getTodayFocus());
    f.setTomorrowOutlook(dto.getTomorrowOutlook());
    f.setDecisionsNeeded(dto.getDecisionsNeeded());
    report.setFocusOutlook(f);
  }

  private void applyPipeline(Report report, ReportRequest request) {
    report.getPipelineTrackers().clear();
    for (ReportRequest.PipelineTrackerDto dto : request.getPipelineTrackers()) {
      PipelineTracker p = new PipelineTracker();
      p.setReport(report);
      p.setItemId(dto.getItemId());
      p.setDataPrep(dto.getDataPrep());
      p.setModelDev(dto.getModelDev());
      p.setIntegration(dto.getIntegration());
      p.setValidation(dto.getValidation());
      p.setDeployment(dto.getDeployment());
      p.setNotes(dto.getNotes());
      report.getPipelineTrackers().add(p);
    }
  }

  private void applyWorkItems(Report report, ReportRequest request) {
    report.getWorkItems().clear();
    for (ReportRequest.WorkItemDto dto : request.getWorkItems()) {
      WorkItem w = new WorkItem();
      w.setReport(report);
      w.setMemberName(dto.getMemberName());
      w.setStoryId(dto.getStoryId());
      w.setTaskDescription(dto.getTaskDescription());
      w.setStatus(dto.getStatus());
      w.setAgeDays(dto.getAgeDays());
      w.setFlag(dto.getFlag());
      report.getWorkItems().add(w);
    }
  }

  private void applyBlockers(Report report, ReportRequest request) {
    report.getBlockersDependencies().clear();
    for (ReportRequest.BlockerDependencyDto dto : request.getBlockersDependencies()) {
      BlockerDependency b = new BlockerDependency();
      b.setReport(report);
      b.setItemId(dto.getItemId());
      b.setType(dto.getType());
      b.setDescription(dto.getDescription());
      b.setOwner(dto.getOwner());
      b.setActionNeeded(dto.getActionNeeded());
      report.getBlockersDependencies().add(b);
    }
  }

  private void applyRisks(Report report, ReportRequest request) {
    report.getRisksObservations().clear();
    for (ReportRequest.RiskObservationDto dto : request.getRisksObservations()) {
      RiskObservation r = new RiskObservation();
      r.setReport(report);
      r.setDescription(dto.getDescription());
      report.getRisksObservations().add(r);
    }
  }

  public ReportResponse toResponse(Report report) {
    return ReportResponse.builder()
        .id(report.getId())
        .podName(report.getPodName())
        .date(report.getDate())
        .sprintNumber(report.getSprintNumber())
        .dayOfSprint(report.getDayOfSprint())
        .goal(report.getGoal())
        .scrumMasterName(report.getScrumMasterName())
        .managerName(report.getManagerName())
        .versionNumber(report.getVersionNumber())
        .warningText(report.getWarningText())
        .createdBy(report.getCreatedBy() != null ? report.getCreatedBy().getEmail() : null)
        .createdAt(report.getCreatedAt())
        .sprintBurndown(toSprint(report.getSprintBurndown()))
        .doraMetrics(toDora(report.getDoraMetrics()))
        .focusOutlook(toFocus(report.getFocusOutlook()))
        .pipelineTrackers(report.getPipelineTrackers().stream().map(this::toPipeline).collect(Collectors.toList()))
        .workItems(report.getWorkItems().stream().map(this::toWork).collect(Collectors.toList()))
        .blockersDependencies(report.getBlockersDependencies().stream().map(this::toBlocker).collect(Collectors.toList()))
        .risksObservations(report.getRisksObservations().stream().map(this::toRisk).collect(Collectors.toList()))
        .build();
  }

  private ReportRequest.SprintBurndownDto toSprint(SprintBurndown s) {
    if (s == null) return null;
    ReportRequest.SprintBurndownDto dto = new ReportRequest.SprintBurndownDto();
    dto.setTotalStories(s.getTotalStories());
    dto.setCompleted(s.getCompleted());
    dto.setInProgress(s.getInProgress());
    dto.setRemainingSp(s.getRemainingSp());
    dto.setIdealSp(s.getIdealSp());
    dto.setVariance(s.getVariance());
    dto.setTrend(s.getTrend());
    return dto;
  }

  private ReportRequest.DoraMetricsDto toDora(DoraMetrics d) {
    if (d == null) return null;
    ReportRequest.DoraMetricsDto dto = new ReportRequest.DoraMetricsDto();
    dto.setDeploymentFrequencyToday(d.getDeploymentFrequencyToday());
    dto.setDeploymentFrequencySprintAvg(d.getDeploymentFrequencySprintAvg());
    dto.setDeploymentFrequencyTarget(d.getDeploymentFrequencyTarget());
    dto.setLeadTimeToday(d.getLeadTimeToday());
    dto.setLeadTimeAvg(d.getLeadTimeAvg());
    dto.setLeadTimeTarget(d.getLeadTimeTarget());
    dto.setChangeFailureRateToday(d.getChangeFailureRateToday());
    dto.setChangeFailureRateAvg(d.getChangeFailureRateAvg());
    dto.setChangeFailureRateTarget(d.getChangeFailureRateTarget());
    dto.setMttrToday(d.getMttrToday());
    dto.setMttrAvg(d.getMttrAvg());
    dto.setMttrTarget(d.getMttrTarget());
    return dto;
  }

  private ReportRequest.FocusOutlookDto toFocus(FocusOutlook f) {
    if (f == null) return null;
    ReportRequest.FocusOutlookDto dto = new ReportRequest.FocusOutlookDto();
    dto.setTodayFocus(f.getTodayFocus());
    dto.setTomorrowOutlook(f.getTomorrowOutlook());
    dto.setDecisionsNeeded(f.getDecisionsNeeded());
    return dto;
  }

  private ReportRequest.PipelineTrackerDto toPipeline(PipelineTracker p) {
    ReportRequest.PipelineTrackerDto dto = new ReportRequest.PipelineTrackerDto();
    dto.setItemId(p.getItemId());
    dto.setDataPrep(p.getDataPrep());
    dto.setModelDev(p.getModelDev());
    dto.setIntegration(p.getIntegration());
    dto.setValidation(p.getValidation());
    dto.setDeployment(p.getDeployment());
    dto.setNotes(p.getNotes());
    return dto;
  }

  private ReportRequest.WorkItemDto toWork(WorkItem w) {
    ReportRequest.WorkItemDto dto = new ReportRequest.WorkItemDto();
    dto.setMemberName(w.getMemberName());
    dto.setStoryId(w.getStoryId());
    dto.setTaskDescription(w.getTaskDescription());
    dto.setStatus(w.getStatus());
    dto.setAgeDays(w.getAgeDays());
    dto.setFlag(w.getFlag());
    return dto;
  }

  private ReportRequest.BlockerDependencyDto toBlocker(BlockerDependency b) {
    ReportRequest.BlockerDependencyDto dto = new ReportRequest.BlockerDependencyDto();
    dto.setItemId(b.getItemId());
    dto.setType(b.getType());
    dto.setDescription(b.getDescription());
    dto.setOwner(b.getOwner());
    dto.setActionNeeded(b.getActionNeeded());
    return dto;
  }

  private ReportRequest.RiskObservationDto toRisk(RiskObservation r) {
    ReportRequest.RiskObservationDto dto = new ReportRequest.RiskObservationDto();
    dto.setDescription(r.getDescription());
    return dto;
  }
}
