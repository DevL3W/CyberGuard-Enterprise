package com.cyberguard.repository;

import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByCompanyIdOrderByNameAsc(Long companyId);
    Optional<Device> findByIdAndCompanyId(Long id, Long companyId);
    List<Device> findByStatusAndLastSeenBefore(DeviceStatus status, Instant before);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndStatus(Long companyId, DeviceStatus status);
}
