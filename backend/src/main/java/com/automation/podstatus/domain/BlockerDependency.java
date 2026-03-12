package com.automation.podstatus.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "blockers_dependencies")
public class BlockerDependency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private Report report;

  @Column(name = "item_id")
  private String itemId;

  @Enumerated(EnumType.STRING)
  private BlockerType type;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String owner;

  @Column(name = "action_needed", columnDefinition = "TEXT")
  private String actionNeeded;
}
