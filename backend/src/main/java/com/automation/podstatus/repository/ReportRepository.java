package com.automation.podstatus.repository;

import com.automation.podstatus.domain.Report;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

  List<Report> findBySprintNumberOrderByDateDesc(Integer sprintNumber);

  List<Report> findByDateOrderByCreatedAtDesc(LocalDate date);

  @Override
  List<Report> findAll();
}
