package com.cyberguard.service;

import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.RegisterRequest;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JwtService;
import com.cyberguard.security.RegistrationRateLimiter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Registro público: crea una empresa nueva y su primera sesión de administrador. */
@Service
public class RegistrationService {
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RegistrationRateLimiter limiter;
    private final AuditService audit;
    private final boolean enabled;

    public RegistrationService(CompanyRepository companies, AppUserRepository users, PasswordEncoder encoder,
                               JwtService jwt, RegistrationRateLimiter limiter, AuditService audit,
                               @Value("${cyberguard.registration.enabled}") boolean enabled) {
        this.companies = companies; this.users = users; this.encoder = encoder;
        this.jwt = jwt; this.limiter = limiter; this.audit = audit; this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    @Transactional
    public LoginResponse register(String clientAddress, RegisterRequest req) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El registro de cuentas está desactivado");
        }
        if (!limiter.allow(clientAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados registros desde esta conexión. Inténtelo más tarde");
        }
        String username = req.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de sesión ya existe");
        }
        Company company = companies.save(new Company(req.companyName().trim(),
                "CG-" + UUID.randomUUID().toString().toUpperCase()));
        AppUser admin = users.save(new AppUser(company, username, encoder.encode(req.password()),
                req.fullName().trim(), Role.ADMIN));
        audit.record(company.getId(), admin.getId(), username, "REGISTRO", "Empresa", company.getId(), company.getName());
        String token = jwt.generate(AppUserDetails.from(admin));
        return new LoginResponse(token, jwt.getExpirationMinutes(), UserDto.from(admin));
    }
}
