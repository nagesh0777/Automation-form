package com.automation.podstatus.controller;

import com.automation.podstatus.domain.ExportHistory;
import com.automation.podstatus.domain.ReportVersion;
import com.automation.podstatus.dto.ReportRequest;
import com.automation.podstatus.dto.ReportResponse;
import com.automation.podstatus.service.DocumentGenerationService;
import com.automation.podstatus.service.ReportService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final ReportService reportService;
  private final DocumentGenerationService documentGenerationService;

  public ReportController(ReportService reportService, DocumentGenerationService documentGenerationService) {
    this.reportService = reportService;
    this.documentGenerationService = documentGenerationService;
  }

  @PostMapping
  public ResponseEntity<ReportResponse> create(@Valid @RequestBody ReportRequest request) {
    return ResponseEntity.ok(reportService.create(request, currentUserEmail()));
  }

  @GetMapping
  public ResponseEntity<List<ReportResponse>> list(
      @RequestParam(required = false) Integer sprint,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(reportService.list(sprint, date));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReportResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(reportService.get(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ReportResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ReportRequest request) {
    return ResponseEntity.ok(reportService.update(id, request, currentUserEmail()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    reportService.delete(id, currentUserEmail());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/generate-docx")
  public ResponseEntity<byte[]> generateDocx(@PathVariable Long id) {
    byte[] bytes = documentGenerationService.generateDocx(reportService.findEntity(id));
    reportService.logExport(id, "DOCX", currentUserEmail());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    headers.setContentDisposition(ContentDisposition.attachment().filename("daily-pod-status-" + id + ".docx").build());
    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  @PostMapping("/{id}/generate-pdf")
  public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
    byte[] bytes = documentGenerationService.generatePdf(reportService.findEntity(id));
    reportService.logExport(id, "PDF", currentUserEmail());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment().filename("daily-pod-status-" + id + ".pdf").build());
    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  @GetMapping("/{id}/printable-html")
  public ResponseEntity<String> printableHtml(@PathVariable Long id) {
    String html = documentGenerationService.generateHtml(reportService.findEntity(id));
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  @GetMapping("/{id}/exports")
  public ResponseEntity<List<ExportHistory>> exports(@PathVariable Long id) {
    return ResponseEntity.ok(reportService.exports(id));
  }

  @GetMapping("/{id}/versions")
  public ResponseEntity<List<ReportVersion>> versions(@PathVariable Long id) {
    return ResponseEntity.ok(reportService.versions(id));
  }

  @PostMapping("/{id}/share/slack")
  public ResponseEntity<Map<String, String>> shareSlack(@PathVariable Long id) {
    return ResponseEntity.ok(Map.of("message", "Slack integration hook placeholder for report " + id));
  }

  @PostMapping("/{id}/share/email")
  public ResponseEntity<Map<String, String>> shareEmail(@PathVariable Long id) {
    return ResponseEntity.ok(Map.of("message", "Email integration hook placeholder for report " + id));
  }

  private String currentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "system@local";
    }
    String name = authentication.getName();
    if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
      return "system@local";
    }
    return name;
  }
}
