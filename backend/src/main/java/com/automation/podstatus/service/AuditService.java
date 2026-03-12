package com.automation.podstatus.service;

import com.automation.podstatus.domain.AuditLog;
import com.automation.podstatus.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public void log(String userEmail, String action, String entityName, String entityId, String metadata) {
    AuditLog log = new AuditLog();
    log.setUserEmail(userEmail != null ? userEmail : "system");
    log.setAction(action);
    log.setEntityName(entityName);
    log.setEntityId(entityId);
    log.setMetadata(metadata);
    auditLogRepository.save(log);
  }
}
