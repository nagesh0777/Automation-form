package com.automation.podstatus.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.automation.podstatus.domain.BlockerDependency;
import com.automation.podstatus.domain.BlockerType;
import com.automation.podstatus.domain.Report;
import com.automation.podstatus.domain.SprintBurndown;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportAnalyticsServiceTest {

  private final ReportAnalyticsService service = new ReportAnalyticsService();

  @Test
  void computesBurndownWarning() {
    Report report = new Report();
    SprintBurndown sb = new SprintBurndown();
    sb.setVariance(-12.0);
    report.setSprintBurndown(sb);

    String warnings = service.computeWarnings(report);
    assertTrue(warnings.contains("Burndown is off track"));
  }

  @Test
  void detectsBlockerAging() {
    Report report = new Report();
    BlockerDependency blocker = new BlockerDependency();
    blocker.setType(BlockerType.BLOCKER);
    blocker.setDescription("Blocked due to age > 3");
    report.setBlockersDependencies(List.of(blocker));

    String warnings = service.computeWarnings(report);
    assertTrue(warnings.contains("Blocker aging detected"));
  }
}
