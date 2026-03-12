package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "work_items")
public class WorkItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(name = "member_name", nullable = false)
  private String memberName;

  @Column(name = "story_id")
  private String storyId;

  @Column(name = "task_description", columnDefinition = "TEXT")
  private String taskDescription;

  @Enumerated(EnumType.STRING)
  private WorkItemStatus status;

  @Column(name = "age_days")
  private Integer ageDays;

  private String flag;
}
