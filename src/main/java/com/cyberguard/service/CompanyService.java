package com.cyberguard.service;

import com.cyberguard.dto.CompanyDto;
import com.cyberguard.entity.Company;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyService {
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final DeviceRepository devices;
    private final AuditService audit;

    public CompanyService(CompanyRepository companies, AppUserRepository users, DeviceRepository devices,
                          AuditService audit) {
        this.companies = companies; this.users = users; this.devices = devices; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public CompanyDto get(AppUserDetails actor) {
        return toDto(find(actor), actor.getCompanyId());
    }

    @Transactional
    public CompanyDto rename(AppUserDetails actor, String name) {
        Company c = find(actor);
        String old = c.getName();
        c.setName(name.trim());
        audit.record(actor, "MODIFICAR", "Empresa", actor.getCompanyId(), old + " → " + c.getName());
        return toDto(c, actor.getCompanyId());
    }

    private Company find(AppUserDetails actor) {
        return companies.findById(actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    }

    private CompanyDto toDto(Company c, Long companyId) {
        return new CompanyDto(c.getName(), c.getLicenseKey(), c.isActive(), c.getCreatedAt(),
                users.countByCompanyId(companyId), devices.countByCompanyId(companyId));
    }
}
