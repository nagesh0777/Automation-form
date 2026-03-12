package com.automation.podstatus.service;

import com.automation.podstatus.domain.ExportHistory;
import com.automation.podstatus.domain.Report;
import com.automation.podstatus.domain.ReportVersion;
import com.automation.podstatus.domain.Role;
import com.automation.podstatus.domain.User;
import com.automation.podstatus.dto.ReportRequest;
import com.automation.podstatus.dto.ReportResponse;
import com.automation.podstatus.repository.ExportHistoryRepository;
import com.automation.podstatus.repository.ReportRepository;
import com.automation.podstatus.repository.ReportVersionRepository;
import com.automation.podstatus.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  private final ReportRepository reportRepository;
  private final UserRepository userRepository;
  private final ReportVersionRepository reportVersionRepository;
  private final ExportHistoryRepository exportHistoryRepository;
  private final ReportMapper mapper;
  private final ReportAnalyticsService analyticsService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public ReportService(ReportRepository reportRepository,
                       UserRepository userRepository,
                       ReportVersionRepository reportVersionRepository,
                       ExportHistoryRepository exportHistoryRepository,
                       ReportMapper mapper,
                       ReportAnalyticsService analyticsService,
                       AuditService auditService,
                       ObjectMapper objectMapper) {
    this.reportRepository = reportRepository;
    this.userRepository = userRepository;
    this.reportVersionRepository = reportVersionRepository;
    this.exportHistoryRepository = exportHistoryRepository;
    this.mapper = mapper;
    this.analyticsService = analyticsService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ReportResponse create(ReportRequest request, String currentUserEmail) {
    User user = resolveOrCreateUser(currentUserEmail);

    Report report = new Report();
    report.setCreatedBy(user);
    mapper.updateEntity(report, request);
    report.setWarningText(analyticsService.computeWarnings(report));

    Report saved = reportRepository.save(report);
    saveVersion(saved, request, user.getEmail(), 1);
    auditService.log(currentUserEmail, "CREATE", "Report", String.valueOf(saved.getId()), "Report created");
    return mapper.toResponse(saved);
  }

  @Transactional
  public ReportResponse update(Long reportId, ReportRequest request, String currentUserEmail) {
    Report report = findEntity(reportId);
    mapper.updateEntity(report, request);
    report.setVersionNumber(report.getVersionNumber() + 1);
    report.setWarningText(analyticsService.computeWarnings(report));

    Report saved = reportRepository.save(report);
    saveVersion(saved, request, currentUserEmail, saved.getVersionNumber());
    auditService.log(currentUserEmail, "UPDATE", "Report", String.valueOf(saved.getId()), "Report updated");
    return mapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<ReportResponse> list(Integer sprint, LocalDate date) {
    List<Report> reports;
    if (sprint != null) {
      reports = reportRepository.findBySprintNumberOrderByDateDesc(sprint);
    } else if (date != null) {
      reports = reportRepository.findByDateOrderByCreatedAtDesc(date);
    } else {
      reports = reportRepository.findAll();
    }
    return reports.stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ReportResponse get(Long reportId) {
    return mapper.toResponse(findEntity(reportId));
  }

  @Transactional
  public void delete(Long reportId, String currentUserEmail) {
    reportRepository.delete(findEntity(reportId));
    auditService.log(currentUserEmail, "DELETE", "Report", String.valueOf(reportId), "Report deleted");
  }

  @Transactional(readOnly = true)
  public Report findEntity(Long id) {
    Report report = reportRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));

    // Force full initialization for stable downstream document generation.
    Hibernate.initialize(report.getPipelineTrackers());
    Hibernate.initialize(report.getWorkItems());
    Hibernate.initialize(report.getBlockersDependencies());
    Hibernate.initialize(report.getRisksObservations());

    report.getPipelineTrackers().sort(Comparator.comparing(p -> p.getId() == null ? Long.MAX_VALUE : p.getId()));
    report.getWorkItems().sort(Comparator.comparing(w -> w.getId() == null ? Long.MAX_VALUE : w.getId()));
    report.getBlockersDependencies().sort(Comparator.comparing(b -> b.getId() == null ? Long.MAX_VALUE : b.getId()));
    report.getRisksObservations().sort(Comparator.comparing(r -> r.getId() == null ? Long.MAX_VALUE : r.getId()));
    return report;
  }

  @Transactional
  public void logExport(Long reportId, String format, String generatedBy) {
    Report report = findEntity(reportId);
    ExportHistory history = new ExportHistory();
    history.setReport(report);
    history.setFormat(format);
    history.setGeneratedBy(generatedBy);
    exportHistoryRepository.save(history);
    auditService.log(generatedBy, "EXPORT_" + format, "Report", String.valueOf(reportId), "Report exported");
  }

  @Transactional(readOnly = true)
  public List<ExportHistory> exports(Long reportId) {
    return exportHistoryRepository.findByReportIdOrderByGeneratedAtDesc(reportId);
  }

  @Transactional(readOnly = true)
  public List<ReportVersion> versions(Long reportId) {
    return reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId);
  }

  private void saveVersion(Report report, ReportRequest request, String createdBy, int version) {
    ReportVersion rv = new ReportVersion();
    rv.setReport(report);
    rv.setVersionNumber(version);
    rv.setCreatedBy(createdBy);
    rv.setPayloadJson(toJson(request));
    reportVersionRepository.save(rv);
  }

  private String toJson(ReportRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize report payload", e);
    }
  }

  private User resolveOrCreateUser(String currentUserEmail) {
    String email = (currentUserEmail == null || currentUserEmail.isBlank())
        ? "system@local"
        : currentUserEmail;

    return userRepository.findByEmail(email).orElseGet(() -> {
      User systemUser = new User();
      systemUser.setName("System User");
      systemUser.setEmail(email);
      systemUser.setRole(Role.SCRUM_MASTER);
      // Non-interactive service identity used for local automation flows.
      systemUser.setPassword("system-generated");
      return userRepository.save(systemUser);
    });
  }
}
