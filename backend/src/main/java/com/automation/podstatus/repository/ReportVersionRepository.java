package com.automation.podstatus.repository;

import com.automation.podstatus.domain.ReportVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, Long> {
  List<ReportVersion> findByReportIdOrderByVersionNumberDesc(Long reportId);
}
