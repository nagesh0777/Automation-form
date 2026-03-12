package com.automation.podstatus.service;

import com.automation.podstatus.domain.BlockerDependency;
import com.automation.podstatus.domain.DoraMetrics;
import com.automation.podstatus.domain.FocusOutlook;
import com.automation.podstatus.domain.PipelineStatus;
import com.automation.podstatus.domain.PipelineTracker;
import com.automation.podstatus.domain.Report;
import com.automation.podstatus.domain.RiskObservation;
import com.automation.podstatus.domain.SprintBurndown;
import com.automation.podstatus.domain.WorkItem;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DocumentGenerationService {

  private final String templatePath;

  public DocumentGenerationService(@Value("${app.template.docx-path}") String templatePath) {
    this.templatePath = templatePath;
  }

  public byte[] generateDocx(Report report) {
    try {
      InputStream templateStream = resolveTemplateStream();
      try (XWPFDocument document = new XWPFDocument(templateStream);
           ByteArrayOutputStream out = new ByteArrayOutputStream()) {

        fillExactTemplate(document, report);

        // Optional replacement support when explicit {{placeholders}} are present in the template.
        Map<String, String> placeholders = placeholderMap(report);
        replaceInParagraphs(document.getParagraphs(), placeholders);
        for (XWPFHeader header : document.getHeaderList()) {
          replaceInParagraphs(header.getParagraphs(), placeholders);
          replaceInTables(header.getTables(), placeholders);
        }
        for (XWPFFooter footer : document.getFooterList()) {
          replaceInParagraphs(footer.getParagraphs(), placeholders);
          replaceInTables(footer.getTables(), placeholders);
        }
        replaceInTables(document.getTables(), placeholders);

        document.write(out);
        return out.toByteArray();
      }
    } catch (IOException ex) {
      throw new IllegalArgumentException("DOCX generation failed. Ensure template exists at " + templatePath, ex);
    }
  }

  private void fillExactTemplate(XWPFDocument document, Report report) {
    List<XWPFTable> tables = document.getTables();
    if (tables.size() < 7) {
      return;
    }

    // Header table
    setCell(tables.get(0), 0, 0, "Date: " + v(report.getDate()));
    setCell(tables.get(0), 0, 1, "Sprint: " + v(report.getSprintNumber()));
    setCell(tables.get(0), 0, 2, "Day: " + v(report.getDayOfSprint()));
    setCell(tables.get(0), 0, 3, "Goal: " + v(report.getGoal()));

    // 1. Burndown table
    SprintBurndown sb = report.getSprintBurndown();
    setCell(tables.get(1), 1, 0, v(sb != null ? sb.getTotalStories() : null));
    setCell(tables.get(1), 1, 1, v(sb != null ? sb.getCompleted() : null));
    setCell(tables.get(1), 1, 2, v(sb != null ? sb.getInProgress() : null));
    setCell(tables.get(1), 1, 3, v(sb != null ? sb.getRemainingSp() : null));
    setCell(tables.get(1), 1, 4, v(sb != null ? sb.getIdealSp() : null));
    setCell(tables.get(1), 1, 5, v(sb != null ? sb.getVariance() : null));

    // 2. DORA table
    DoraMetrics dm = report.getDoraMetrics();
    setCell(tables.get(2), 1, 1, v(dm != null ? dm.getDeploymentFrequencyToday() : null));
    setCell(tables.get(2), 1, 2, v(dm != null ? dm.getDeploymentFrequencySprintAvg() : null));
    setCell(tables.get(2), 1, 3, v(dm != null ? dm.getDeploymentFrequencyTarget() : null));

    setCell(tables.get(2), 2, 1, v(dm != null ? dm.getLeadTimeToday() : null));
    setCell(tables.get(2), 2, 2, v(dm != null ? dm.getLeadTimeAvg() : null));
    setCell(tables.get(2), 2, 3, v(dm != null ? dm.getLeadTimeTarget() : null));

    setCell(tables.get(2), 3, 1, v(dm != null ? dm.getChangeFailureRateToday() : null));
    setCell(tables.get(2), 3, 2, v(dm != null ? dm.getChangeFailureRateAvg() : null));
    setCell(tables.get(2), 3, 3, v(dm != null ? dm.getChangeFailureRateTarget() : null));

    setCell(tables.get(2), 4, 1, v(dm != null ? dm.getMttrToday() : null));
    setCell(tables.get(2), 4, 2, v(dm != null ? dm.getMttrAvg() : null));
    setCell(tables.get(2), 4, 3, v(dm != null ? dm.getMttrTarget() : null));

    // 3. Pipeline table: data starts row 2 (0-based index)
    XWPFTable pipelineTable = tables.get(3);
    int pipelineRows = ensureDataRows(pipelineTable, 1, 7, report.getPipelineTrackers().size());
    for (int i = 0; i < pipelineRows; i++) {
      PipelineTracker p = i < report.getPipelineTrackers().size() ? report.getPipelineTrackers().get(i) : null;
      setCell(pipelineTable, i + 1, 0, v(p != null ? p.getItemId() : null));
      setStatusCell(pipelineTable, i + 1, 1, p != null ? p.getDataPrep() : null);
      setStatusCell(pipelineTable, i + 1, 2, p != null ? p.getModelDev() : null);
      setStatusCell(pipelineTable, i + 1, 3, p != null ? p.getIntegration() : null);
      setStatusCell(pipelineTable, i + 1, 4, p != null ? p.getValidation() : null);
      setStatusCell(pipelineTable, i + 1, 5, p != null ? p.getDeployment() : null);
      setCell(pipelineTable, i + 1, 6, v(p != null ? p.getNotes() : null));
    }

    // 4. Work items table
    XWPFTable workTable = tables.get(4);
    int workRows = ensureDataRows(workTable, 1, 6, report.getWorkItems().size());
    for (int i = 0; i < workRows; i++) {
      WorkItem w = i < report.getWorkItems().size() ? report.getWorkItems().get(i) : null;
      setCell(workTable, i + 1, 0, v(w != null ? w.getMemberName() : null));
      setCell(workTable, i + 1, 1, v(w != null ? w.getStoryId() : null));
      setCell(workTable, i + 1, 2, v(w != null ? w.getTaskDescription() : null));
      setCell(workTable, i + 1, 3, v(w != null ? w.getStatus() : null));
      setCell(workTable, i + 1, 4, v(w != null ? w.getAgeDays() : null));
      setCell(workTable, i + 1, 5, v(w != null ? w.getFlag() : null));
    }

    // 5. Blockers/dependencies table
    XWPFTable blockerTable = tables.get(5);
    int blockerRows = ensureDataRows(blockerTable, 1, 5, report.getBlockersDependencies().size());
    for (int i = 0; i < blockerRows; i++) {
      BlockerDependency b = i < report.getBlockersDependencies().size() ? report.getBlockersDependencies().get(i) : null;
      setCell(blockerTable, i + 1, 0, v(b != null ? b.getItemId() : null));
      setCell(blockerTable, i + 1, 1, v(b != null ? b.getType() : null));
      setCell(blockerTable, i + 1, 2, v(b != null ? b.getDescription() : null));
      setCell(blockerTable, i + 1, 3, v(b != null ? b.getOwner() : null));
      setCell(blockerTable, i + 1, 4, v(b != null ? b.getActionNeeded() : null));
    }

    // 7. Focus/outlook table
    XWPFTable focusTable = tables.get(6);
    FocusOutlook fo = report.getFocusOutlook();
    setCell(focusTable, 0, 1, v(fo != null ? fo.getTodayFocus() : null));
    setCell(focusTable, 1, 1, v(fo != null ? fo.getTomorrowOutlook() : null));
    setCell(focusTable, 2, 1, v(fo != null ? fo.getDecisionsNeeded() : null));

    // Paragraph-level replacements for title, trend, risks, legend, footer.
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    replaceParagraphContaining(paragraphs, "[POD NAME]", v(report.getPodName()));
    replaceParagraphContaining(paragraphs, "Date: [YYYY-MM-DD]", "Date: " + v(report.getDate()));
    replaceParagraphContaining(paragraphs, "Sprint: [X of Y]", "Sprint: " + v(report.getSprintNumber()));
    replaceParagraphContaining(paragraphs, "Day: [N of Sprint]", "Day: " + v(report.getDayOfSprint()));
    replaceParagraphContaining(paragraphs, "Goal: [Sprint Goal]", "Goal: " + v(report.getGoal()));

    replaceParagraphContaining(paragraphs,
        "Burndown Trend:",
        "Burndown Trend: " + v(sb != null ? sb.getTrend() : null));

    replaceParagraphContaining(paragraphs,
        "Legend:",
        "Legend:  \u2588\u2588 Complete  \u2593\u2593 In Progress  \u2591\u2591 Not Started  \u25A0\u25A0 Blocked");

    replaceParagraphContaining(paragraphs,
        "Quality Gate Notes:",
        "Quality Gate Notes: " + v(report.getWarningText()));

    replaceParagraphContaining(paragraphs,
        "Prepared by:",
        "Prepared by: " + v(report.getScrumMasterName()) + " |    Distribution: " + v(report.getManagerName()));

    int riskIdx = 0;
    XWPFParagraph lastRiskParagraph = null;
    for (XWPFParagraph p : paragraphs) {
      String text = p.getText();
      if (text == null) {
        continue;
      }
      if (text.contains("[Risk or observation with impact assessment]")
          || text.contains("[Technical debt or process observation]")
          || text.contains("[Team health signal")) {
        String riskText = riskIdx < report.getRisksObservations().size()
            ? v(report.getRisksObservations().get(riskIdx).getDescription())
            : "";
        setParagraphText(p, riskText);
        lastRiskParagraph = p;
        riskIdx++;
      }
    }
    appendOverflowRisks(document, report, riskIdx, lastRiskParagraph);
  }

  public byte[] generatePdf(Report report) {
    String html = generateHtml(report);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.toStream(output);
      builder.run();
      return output.toByteArray();
    } catch (Exception e) {
      throw new IllegalArgumentException("PDF generation failed", e);
    }
  }

  public String generateHtml(Report report) {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>Daily POD Status Report</title>")
        .append("<style>")
        .append("body{font-family:Arial,sans-serif;font-size:12px;padding:28px;color:#111;}h1{font-size:18px;}h2{font-size:14px;margin-top:16px;}table{width:100%;border-collapse:collapse;margin-top:8px;}th,td{border:1px solid #333;padding:6px;vertical-align:top;}footer{margin-top:20px;font-size:11px;} .legend{margin-top:12px;white-space:pre-line;} @media print{body{padding:0;}}")
        .append("</style></head><body>");

    html.append("<h1>Daily POD Status Report</h1>")
        .append("<p><b>POD:</b> ").append(h(report.getPodName())).append(" | <b>Date:</b> ").append(h(report.getDate())).append(" | <b>Sprint:</b> ").append(h(report.getSprintNumber())).append(" | <b>Day:</b> ").append(h(report.getDayOfSprint())).append("</p>")
        .append("<p><b>Goal:</b> ").append(h(report.getGoal())).append("</p>");

    html.append("<h2>1. SPRINT BURNDOWN SNAPSHOT</h2>");
    html.append("<table><tr><th>Total Stories</th><th>Completed</th><th>In Progress</th><th>Remaining SP</th><th>Ideal SP</th><th>Variance</th><th>Trend</th></tr>");
    SprintBurndown sb = report.getSprintBurndown();
    html.append("<tr><td>").append(h(sb != null ? sb.getTotalStories() : null)).append("</td><td>").append(h(sb != null ? sb.getCompleted() : null)).append("</td><td>").append(h(sb != null ? sb.getInProgress() : null)).append("</td><td>").append(h(sb != null ? sb.getRemainingSp() : null)).append("</td><td>").append(h(sb != null ? sb.getIdealSp() : null)).append("</td><td>").append(h(sb != null ? sb.getVariance() : null)).append("</td><td>").append(h(sb != null ? sb.getTrend() : null)).append("</td></tr></table>");

    html.append("<h2>2. DORA METRICS (DAILY PULSE)</h2>");
    html.append("<table><tr><th>Metric</th><th>Today</th><th>Sprint Avg</th><th>Target</th></tr>");
    DoraMetrics dm = report.getDoraMetrics();
    row(html, "Deployment Frequency", dm != null ? dm.getDeploymentFrequencyToday() : null, dm != null ? dm.getDeploymentFrequencySprintAvg() : null, dm != null ? dm.getDeploymentFrequencyTarget() : null);
    row(html, "Lead Time", dm != null ? dm.getLeadTimeToday() : null, dm != null ? dm.getLeadTimeAvg() : null, dm != null ? dm.getLeadTimeTarget() : null);
    row(html, "Change Failure Rate", dm != null ? dm.getChangeFailureRateToday() : null, dm != null ? dm.getChangeFailureRateAvg() : null, dm != null ? dm.getChangeFailureRateTarget() : null);
    row(html, "MTTR", dm != null ? dm.getMttrToday() : null, dm != null ? dm.getMttrAvg() : null, dm != null ? dm.getMttrTarget() : null);
    html.append("</table>");

    html.append("<h2>3. PIPELINE STAGE TRACKER</h2>");
    html.append("<table><tr><th>Item</th><th>Data Prep</th><th>Model Dev</th><th>Integration</th><th>Validation</th><th>Deployment</th><th>Notes</th></tr>");
    for (PipelineTracker p : report.getPipelineTrackers()) {
      html.append("<tr><td>").append(h(p.getItemId())).append("</td><td>").append(statusHtml(p.getDataPrep())).append("</td><td>").append(statusHtml(p.getModelDev())).append("</td><td>").append(statusHtml(p.getIntegration())).append("</td><td>").append(statusHtml(p.getValidation())).append("</td><td>").append(statusHtml(p.getDeployment())).append("</td><td>").append(h(p.getNotes())).append("</td></tr>");
    }
    html.append("</table>");

    html.append("<h2>4. WORK ITEM DETAIL (BY TEAM MEMBER)</h2>");
    html.append("<table><tr><th>Member</th><th>Story ID</th><th>Task</th><th>Status</th><th>Age (days)</th><th>Flag</th></tr>");
    for (WorkItem w : report.getWorkItems()) {
      html.append("<tr><td>").append(h(w.getMemberName())).append("</td><td>").append(h(w.getStoryId())).append("</td><td>").append(h(w.getTaskDescription())).append("</td><td>").append(h(w.getStatus())).append("</td><td>").append(h(w.getAgeDays())).append("</td><td>").append(h(w.getFlag())).append("</td></tr>");
    }
    html.append("</table>");

    html.append("<h2>5. BLOCKERS &amp; DEPENDENCIES</h2>");
    html.append("<table><tr><th>ID</th><th>Type</th><th>Description</th><th>Owner</th><th>Action Needed</th></tr>");
    for (BlockerDependency b : report.getBlockersDependencies()) {
      html.append("<tr><td>").append(h(b.getItemId())).append("</td><td>").append(h(b.getType())).append("</td><td>").append(h(b.getDescription())).append("</td><td>").append(h(b.getOwner())).append("</td><td>").append(h(b.getActionNeeded())).append("</td></tr>");
    }
    html.append("</table>");

    html.append("<h2>6. RISKS &amp; OBSERVATIONS</h2><ul>");
    for (RiskObservation r : report.getRisksObservations()) {
      html.append("<li>").append(h(r.getDescription())).append("</li>");
    }
    html.append("</ul>");

    html.append("<h2>7. TODAY'S FOCUS &amp; TOMORROW'S OUTLOOK</h2>");
    FocusOutlook fo = report.getFocusOutlook();
    html.append("<table><tr><th>Today\u2019s Focus</th><td>").append(h(fo != null ? fo.getTodayFocus() : null)).append("</td></tr>")
        .append("<tr><th>Tomorrow\u2019s Outlook</th><td>").append(h(fo != null ? fo.getTomorrowOutlook() : null)).append("</td></tr>")
        .append("<tr><th>Decisions Needed</th><td>").append(h(fo != null ? fo.getDecisionsNeeded() : null)).append("</td></tr></table>");

    html.append("<div class='legend'><b>Legend:</b><br/>\u2588\u2588 Complete<br/>\u2593\u2593 In Progress<br/>\u2591\u2591 Not Started<br/>\u25A0\u25A0 Blocked</div>");
    html.append("<footer><div>Prepared by: ").append(h(report.getScrumMasterName())).append("</div><div>Distribution: ").append(h(report.getManagerName())).append("</div></footer>");
    html.append("</body></html>");

    return html.toString();
  }

  private void row(StringBuilder html, String metric, Object today, Object avg, Object target) {
    html.append("<tr><td>").append(metric).append("</td><td>").append(h(today)).append("</td><td>")
        .append(h(avg)).append("</td><td>").append(h(target)).append("</td></tr>");
  }

  private InputStream resolveTemplateStream() throws IOException {
    ClassPathResource resource = new ClassPathResource(templatePath);
    if (!resource.exists()) {
      throw new IOException("Template not found: " + templatePath);
    }
    return new ByteArrayInputStream(resource.getContentAsByteArray());
  }

  private void setCell(XWPFTable table, int rowIndex, int colIndex, String text) {
    if (table == null || rowIndex < 0 || rowIndex >= table.getRows().size()) {
      return;
    }
    var row = table.getRow(rowIndex);
    if (row == null || colIndex < 0 || colIndex >= row.getTableCells().size()) {
      return;
    }
    XWPFTableCell cell = row.getCell(colIndex);
    if (cell == null) {
      return;
    }

    if (cell.getParagraphs().isEmpty()) {
      cell.setText(text);
      return;
    }

    XWPFParagraph p = cell.getParagraphs().get(0);
    setParagraphText(p, text);
    for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
      cell.removeParagraph(i);
    }
  }

  private void setStatusCell(XWPFTable table, int rowIndex, int colIndex, PipelineStatus status) {
    if (table == null || rowIndex < 0 || rowIndex >= table.getRows().size()) {
      return;
    }
    XWPFTableRow row = table.getRow(rowIndex);
    if (row == null || colIndex < 0 || colIndex >= row.getTableCells().size()) {
      return;
    }
    XWPFTableCell cell = row.getCell(colIndex);
    if (cell == null) {
      return;
    }
    if (cell.getParagraphs().isEmpty()) {
      cell.addParagraph();
    }
    XWPFParagraph p = cell.getParagraphs().get(0);
    for (int i = p.getRuns().size() - 1; i >= 0; i--) {
      p.removeRun(i);
    }
    XWPFRun run = p.createRun();
    run.setText(statusBlock(status), 0);
    run.setColor(statusColorHex(status));
    run.setBold(true);
    for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
      cell.removeParagraph(i);
    }
  }

  private int ensureDataRows(XWPFTable table, int dataStartRow, int columns, int dataCount) {
    int existingDataRows = Math.max(0, table.getRows().size() - dataStartRow);
    int requiredRows = Math.max(existingDataRows, dataCount);
    while (Math.max(0, table.getRows().size() - dataStartRow) < requiredRows) {
      appendStyledRow(table, dataStartRow, columns);
    }
    return requiredRows;
  }

  private void appendStyledRow(XWPFTable table, int dataStartRow, int columns) {
    int templateRowIndex = table.getRows().size() > dataStartRow
        ? table.getRows().size() - 1
        : Math.max(0, dataStartRow - 1);

    XWPFTableRow templateRow = table.getRow(templateRowIndex);
    XWPFTableRow newRow = table.createRow();

    if (templateRow != null && templateRow.getCtRow().getTrPr() != null) {
      newRow.getCtRow().setTrPr(templateRow.getCtRow().getTrPr());
    }

    while (newRow.getTableCells().size() < columns) {
      newRow.addNewTableCell();
    }

    for (int c = 0; c < columns; c++) {
      XWPFTableCell srcCell = templateRow != null && c < templateRow.getTableCells().size()
          ? templateRow.getCell(c)
          : null;
      XWPFTableCell dstCell = newRow.getCell(c);
      if (srcCell != null && srcCell.getCTTc().getTcPr() != null) {
        dstCell.getCTTc().setTcPr(srcCell.getCTTc().getTcPr());
      }
      setCell(table, table.getRows().size() - 1, c, "");
    }
  }

  private void appendOverflowRisks(XWPFDocument document, Report report, int filledRiskCount, XWPFParagraph anchor) {
    if (report.getRisksObservations().size() <= filledRiskCount) {
      return;
    }
    for (int i = filledRiskCount; i < report.getRisksObservations().size(); i++) {
      String text = v(report.getRisksObservations().get(i).getDescription());
      if (text.isBlank()) {
        continue;
      }
      XWPFParagraph p = document.createParagraph();
      if (anchor != null && anchor.getStyle() != null) {
        p.setStyle(anchor.getStyle());
      }
      setParagraphText(p, text);
    }
  }

  private void replaceParagraphContaining(List<XWPFParagraph> paragraphs, String contains, String replacement) {
    for (XWPFParagraph p : paragraphs) {
      String text = p.getText();
      if (text != null && text.contains(contains)) {
        if (contains.startsWith("[")) {
          setParagraphText(p, text.replace(contains, replacement));
        } else {
          setParagraphText(p, replacement);
        }
      }
    }
  }

  private String statusBlock(PipelineStatus status) {
    if (status == null) {
      return "";
    }
    return switch (status) {
      case COMPLETE -> "\u2588\u2588 Complete";
      case IN_PROGRESS -> "\u2593\u2593 In Progress";
      case NOT_STARTED -> "\u2591\u2591 Not Started";
      case BLOCKED -> "\u25A0\u25A0 Blocked";
    };
  }

  private String statusColorHex(PipelineStatus status) {
    if (status == null) {
      return "6B7280";
    }
    return switch (status) {
      case COMPLETE -> "15803D";
      case IN_PROGRESS -> "B45309";
      case NOT_STARTED -> "6B7280";
      case BLOCKED -> "B91C1C";
    };
  }

  private String statusHtml(PipelineStatus status) {
    return "<span style='font-weight:700;color:#" + statusColorHex(status) + "'>" + h(statusBlock(status)) + "</span>";
  }

  private void setParagraphText(XWPFParagraph paragraph, String text) {
    if (paragraph.getRuns() == null || paragraph.getRuns().isEmpty()) {
      paragraph.createRun().setText(text, 0);
      return;
    }
    XWPFRun first = paragraph.getRuns().get(0);
    first.setText(text, 0);
    for (int i = paragraph.getRuns().size() - 1; i > 0; i--) {
      paragraph.removeRun(i);
    }
  }

  private void replaceInTables(List<XWPFTable> tables, Map<String, String> placeholders) {
    for (XWPFTable table : tables) {
      table.getRows().forEach(row -> row.getTableCells().forEach(cell -> {
        replaceInParagraphs(cell.getParagraphs(), placeholders);
        replaceInTables(cell.getTables(), placeholders);
      }));
    }
  }

  private void replaceInParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> placeholders) {
    for (XWPFParagraph paragraph : paragraphs) {
      if (paragraph.getRuns() == null || paragraph.getRuns().isEmpty()) {
        continue;
      }
      String text = paragraph.getText();
      if (text == null || text.isBlank()) {
        continue;
      }
      String replaced = replace(text, placeholders);
      if (!text.equals(replaced)) {
        setParagraphText(paragraph, replaced);
      }
    }
  }

  private String replace(String content, Map<String, String> placeholders) {
    String output = content;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      output = output.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return output;
  }

  private Map<String, String> placeholderMap(Report report) {
    Map<String, String> map = new HashMap<>();
    map.put("pod_name", v(report.getPodName()));
    map.put("date", v(report.getDate()));
    map.put("sprint_number", v(report.getSprintNumber()));
    map.put("day_of_sprint", v(report.getDayOfSprint()));
    map.put("goal", v(report.getGoal()));
    map.put("prepared_by", v(report.getScrumMasterName()));
    map.put("distribution", v(report.getManagerName()));
    map.put("warning_text", v(report.getWarningText()));
    map.put("legend", "\u2588\u2588 Complete | \u2593\u2593 In Progress | \u2591\u2591 Not Started | \u25A0\u25A0 Blocked");
    return map;
  }

  private String v(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof BigDecimal decimal) {
      BigDecimal normalized = decimal.stripTrailingZeros();
      return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
    if (value instanceof Double || value instanceof Float) {
      BigDecimal normalized = BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros();
      return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
    return String.valueOf(value);
  }

  private String h(Object value) {
    return v(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
