package com.cyberguard.service;

import com.cyberguard.dto.AuditDto;
import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.PasswordChangeRequest;
import com.cyberguard.dto.ProfileDto;
import com.cyberguard.dto.ProfileUpdateRequest;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.AuditLogRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JwtService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Perfil de la propia sesión: datos personales, contraseña, incidentes asignados y actividad reciente. */
@Service
public class ProfileService {
    private final AppUserRepository users;
    private final IncidentRepository incidents;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public ProfileService(AppUserRepository users, IncidentRepository incidents, AuditLogRepository auditLogs,
                          AuditService audit, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.incidents = incidents; this.auditLogs = auditLogs;
        this.audit = audit; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public ProfileDto get(AppUserDetails actor) {
        return build(load(actor), actor);
    }

    @Transactional
    public ProfileDto update(AppUserDetails actor, ProfileUpdateRequest req) {
        AppUser u = load(actor);
        u.setFullName(req.fullName().trim());
        u.setEmail(blankToNull(req.email()));
        u.setJobTitle(blankToNull(req.jobTitle()));
        audit.record(actor, "MODIFICAR", "Perfil", u.getId(), u.getUsername());
        return build(u, actor);
    }

    /**
     * Cambia la contraseña tras comprobar la actual. Las demás sesiones abiertas dejan de valer y se
     * devuelve un token nuevo para la sesión que hizo el cambio.
     */
    @Transactional
    public LoginResponse changePassword(AppUserDetails actor, PasswordChangeRequest req) {
        AppUser u = load(actor);
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña nueva debe ser distinta de la actual");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setPasswordChangedAt(Instant.now());
        audit.record(actor, "CAMBIO_CLAVE", "Sesión", u.getId(), "Cambió su propia contraseña");
        String token = jwt.generate(AppUserDetails.from(u));
        return new LoginResponse(token, jwt.getExpirationMinutes(), UserDto.from(u));
    }

    private AppUser load(AppUserDetails actor) {
        return users.findByIdAndCompanyId(actor.getId(), actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
    }

    private ProfileDto build(AppUser u, AppUserDetails actor) {
        var assigned = incidents.findByCompanyIdAndAssigneeIdAndStatusNotOrderByCreatedAtDesc(
                        actor.getCompanyId(), actor.getId(), IncidentStatus.CERRADO).stream()
                .map(i -> new ProfileDto.IncidentSummary(i.getId(), i.getTitle(), i.getSeverity(), i.getStatus())).toList();
        var activity = auditLogs.findTop15ByCompanyIdAndUserIdOrderByCreatedAtDesc(actor.getCompanyId(), actor.getId())
                .stream().map(AuditDto::from).toList();
        return new ProfileDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getJobTitle(), u.getRole(),
                u.getCompany().getName(), u.getCreatedAt(), assigned, activity);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
