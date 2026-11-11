package com.cyberguard.repository;

import com.cyberguard.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<AuditLog> findTop15ByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, Long userId);
}
