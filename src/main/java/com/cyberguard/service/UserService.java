package com.cyberguard.service;

import com.cyberguard.dto.CreateUserRequest;
import com.cyberguard.dto.DirectoryEntry;
import com.cyberguard.dto.UpdateUserRequest;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.security.AppUserDetails;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Todas las operaciones se limitan a la empresa del usuario autenticado. */
@Service
public class UserService {
    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final String adminUsername;

    public UserService(AppUserRepository users, CompanyRepository companies, PasswordEncoder encoder,
                       AuditService audit, @Value("${cyberguard.admin.username}") String adminUsername) {
        this.users = users; this.companies = companies; this.encoder = encoder;
        this.audit = audit; this.adminUsername = adminUsername;
    }

    /** La sesión principal (admin) no puede desactivarse ni perder su rol. */
    private boolean isProtected(AppUser u) { return u.getUsername().equalsIgnoreCase(adminUsername); }

    private UserDto toDto(AppUser u) { return UserDto.from(u, isProtected(u)); }

    @Transactional(readOnly = true)
    public List<UserDto> list(AppUserDetails actor) {
        return users.findByCompanyIdOrderByFullNameAsc(actor.getCompanyId()).stream().map(this::toDto).toList();
    }

    /** Lista corta (id, nombre, rol) de las sesiones activas, para asignar responsables. */
    @Transactional(readOnly = true)
    public List<DirectoryEntry> directory(AppUserDetails actor) {
        return users.findByCompanyIdOrderByFullNameAsc(actor.getCompanyId()).stream()
                .filter(AppUser::isEnabled).map(DirectoryEntry::from).toList();
    }

    @Transactional
    public UserDto create(AppUserDetails actor, CreateUserRequest req) {
        String username = req.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de sesión ya existe");
        }
        Company company = companies.getReferenceById(actor.getCompanyId());
        AppUser saved = users.save(new AppUser(company, username, encoder.encode(req.password()),
                req.fullName().trim(), req.role()));
        audit.record(actor, "CREAR", "Sesión", saved.getId(), username + " (" + req.role() + ")");
        return toDto(saved);
    }

    /** Un administrador fija una contraseña nueva a otra sesión; sus sesiones abiertas dejan de valer. */
    @Transactional
    public void resetPassword(AppUserDetails actor, Long id, String newPassword) {
        AppUser target = users.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));
        if (target.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cambiar su propia contraseña use Mi perfil");
        }
        if (isProtected(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La contraseña de la sesión principal solo la puede cambiar ella misma");
        }
        target.setPasswordHash(encoder.encode(newPassword));
        target.setPasswordChangedAt(Instant.now());
        audit.record(actor, "CAMBIO_CLAVE", "Sesión", target.getId(), "Contraseña restablecida para " + target.getUsername());
    }

    @Transactional
    public UserDto update(AppUserDetails actor, Long id, UpdateUserRequest req) {
        AppUser target = users.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));
        boolean disables = Boolean.FALSE.equals(req.enabled());
        boolean changesRole = req.role() != null && req.role() != target.getRole();
        if (isProtected(target) && (disables || changesRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La sesión principal no se puede desactivar ni cambiar de rol");
        }
        if (target.getId().equals(actor.getId()) && (disables || changesRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puede desactivar su propia sesión ni cambiar su propio rol");
        }
        if (req.role() != null) target.setRole(req.role());
        if (req.enabled() != null) target.setEnabled(req.enabled());
        audit.record(actor, "MODIFICAR", "Sesión", target.getId(), target.getUsername() + ": rol "
                + target.getRole() + ", " + (target.isEnabled() ? "activa" : "desactivada"));
        return toDto(target);
    }
}
