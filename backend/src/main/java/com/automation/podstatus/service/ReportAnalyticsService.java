package com.automation.podstatus.service;

import com.automation.podstatus.domain.BlockerType;
import com.automation.podstatus.domain.Report;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportAnalyticsService {

  public String computeWarnings(Report report) {
    List<String> warnings = new ArrayList<>();

    if (report.getSprintBurndown() != null && report.getSprintBurndown().getVariance() != null
        && report.getSprintBurndown().getVariance() < -10) {
      warnings.add("Burndown is off track");
    }

    long agingBlockers = report.getBlockersDependencies().stream()
        .filter(b -> b.getType() == BlockerType.BLOCKER)
        .filter(b -> b.getDescription() != null && b.getDescription().toLowerCase().contains("age"))
        .count();

    if (agingBlockers > 0) {
      warnings.add("Blocker aging detected");
    }

    if (report.getDoraMetrics() != null && report.getDoraMetrics().getChangeFailureRateToday() != null
        && report.getDoraMetrics().getChangeFailureRateTarget() != null
        && report.getDoraMetrics().getChangeFailureRateToday() > report.getDoraMetrics().getChangeFailureRateTarget()) {
      warnings.add("Change failure rate above target");
    }

    return String.join("; ", warnings);
  }
}
