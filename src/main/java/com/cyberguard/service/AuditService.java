package com.cyberguard.service;

import com.cyberguard.dto.AuditDto;
import com.cyberguard.entity.AuditLog;
import com.cyberguard.repository.AuditLogRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Historial de auditoría: quién hizo qué y cuándo, separado por empresa. */
@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) { this.logs = logs; }

    public void record(AppUserDetails actor, String action, String entityType, Long entityId, String detail) {
        record(actor.getCompanyId(), actor.getId(), actor.getUsername(), action, entityType, entityId, detail);
    }

    public void record(Long companyId, Long userId, String username, String action, String entityType,
                       Long entityId, String detail) {
        logs.save(new AuditLog(companyId, userId, cut(username, 60), action, entityType, entityId, cut(detail, 500)));
    }

    @Transactional(readOnly = true)
    public List<AuditDto> latest(AppUserDetails actor) {
        return logs.findTop200ByCompanyIdOrderByCreatedAtDesc(actor.getCompanyId()).stream().map(AuditDto::from).toList();
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
