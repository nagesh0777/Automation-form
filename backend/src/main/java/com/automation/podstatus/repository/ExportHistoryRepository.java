package com.automation.podstatus.repository;

import com.automation.podstatus.domain.ExportHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportHistoryRepository extends JpaRepository<ExportHistory, Long> {
  List<ExportHistory> findByReportIdOrderByGeneratedAtDesc(Long reportId);
}
