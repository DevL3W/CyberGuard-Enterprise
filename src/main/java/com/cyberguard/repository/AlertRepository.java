package com.cyberguard.repository;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.SecurityAlert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<SecurityAlert> findTop5ByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<SecurityAlert> findByCompanyIdAndCreatedAtAfter(Long companyId, Instant after);
    List<SecurityAlert> findByDeviceIdAndTitleAndStatusNot(Long deviceId, String title, AlertStatus status);
    Optional<SecurityAlert> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndStatusNot(Long companyId, AlertStatus status);
    long countByCompanyIdAndSeverityAndStatusNot(Long companyId, AlertSeverity severity, AlertStatus status);
}
