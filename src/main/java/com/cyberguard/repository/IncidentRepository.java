package com.cyberguard.repository;

import com.cyberguard.entity.Incident;
import com.cyberguard.entity.IncidentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Incident> findByCompanyIdAndAssigneeIdAndStatusNotOrderByCreatedAtDesc(Long companyId, Long assigneeId, IncidentStatus status);
    Optional<Incident> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndStatusNot(Long companyId, IncidentStatus status);
}
