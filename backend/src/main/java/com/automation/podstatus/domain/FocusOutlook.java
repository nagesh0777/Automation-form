package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "focus_outlook")
public class FocusOutlook {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false, unique = true)
  private Report report;

  @Column(name = "today_focus", columnDefinition = "TEXT")
  private String todayFocus;

  @Column(name = "tomorrow_outlook", columnDefinition = "TEXT")
  private String tomorrowOutlook;

  @Column(name = "decisions_needed", columnDefinition = "TEXT")
  private String decisionsNeeded;
}
