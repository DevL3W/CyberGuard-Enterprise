package com.cyberguard.service;

import com.cyberguard.dto.InvitationCreated;
import com.cyberguard.dto.InvitationDto;
import com.cyberguard.dto.InvitationPreview;
import com.cyberguard.dto.InvitationRequest;
import com.cyberguard.dto.JoinRequest;
import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Invitation;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.InvitationRepository;
import com.cyberguard.security.AgentKeys;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JoinRateLimiter;
import com.cyberguard.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Invitaciones por enlace o código QR. El enlace lleva un token aleatorio de 256 bits; en la base de
 * datos solo queda su huella SHA-256, igual que las claves de los agentes.
 */
@Service
public class InvitationService {
    static final String INVALID = "Esta invitación no es válida, ya venció o ya se usó";

    private final InvitationRepository invitations;
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JoinRateLimiter limiter;
    private final AuditService audit;

    public InvitationService(InvitationRepository invitations, CompanyRepository companies, AppUserRepository users,
                             PasswordEncoder encoder, JwtService jwt, JoinRateLimiter limiter, AuditService audit) {
        this.invitations = invitations; this.companies = companies; this.users = users;
        this.encoder = encoder; this.jwt = jwt; this.limiter = limiter; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> list(AppUserDetails actor) {
        Instant now = Instant.now();
        return invitations.findTop100ByCompanyIdOrderByCreatedAtDesc(actor.getCompanyId()).stream()
                .map(i -> InvitationDto.from(i, now)).toList();
    }

    @Transactional
    public InvitationCreated create(AppUserDetails actor, InvitationRequest req) {
        // Una invitación que se pueda reenviar o fotografiar nunca debe dar control total
        if (req.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las invitaciones solo pueden dar el rol Analista o Solo consulta. "
                            + "Para otro administrador cree la sesión desde Sesiones");
        }
        Instant now = Instant.now();
        String token = AgentKeys.generate();
        Invitation saved = invitations.save(new Invitation(actor.getCompanyId(), AgentKeys.hash(token), req.role(),
                req.maxUses(), now.plus(req.days(), ChronoUnit.DAYS), actor.getId(), actor.getUsername()));
        audit.record(actor, "CREAR", "Invitación", saved.getId(),
                req.role() + ", " + req.days() + " días, " + req.maxUses() + " usos");
        return new InvitationCreated(InvitationDto.from(saved, now), token);
    }

    @Transactional
    public void revoke(AppUserDetails actor, Long id) {
        Invitation inv = invitations.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no encontrada"));
        if (!inv.isRevoked()) {
            inv.setRevoked(true);
            audit.record(actor, "REVOCAR", "Invitación", inv.getId(), "Usos: " + inv.getUses() + "/" + inv.getMaxUses());
        }
    }

    /** Lo que ve quien abre el enlace antes de crear su sesión. */
    @Transactional(readOnly = true)
    public InvitationPreview preview(String token) {
        Invitation inv = usable(token);
        return new InvitationPreview(companyOf(inv).getName(), inv.getRole(), inv.getExpiresAt());
    }

    @Transactional
    public LoginResponse accept(String clientAddress, String token, JoinRequest req) {
        if (!limiter.allow(clientAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos desde esta conexión. Inténtelo más tarde");
        }
        Invitation inv = usable(token);
        Company company = companyOf(inv);
        String username = req.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de sesión ya existe");
        }
        Long invitationId = inv.getId();
        Role role = inv.getRole();
        // Gasta el uso antes de crear la sesión; si algo falla después, la transacción lo devuelve
        if (invitations.consume(invitationId, Instant.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID);
        }
        AppUser user = users.save(new AppUser(company, username, encoder.encode(req.password()),
                req.fullName().trim(), role));
        audit.record(company.getId(), user.getId(), username, "UNIRSE", "Sesión", user.getId(),
                "Se unió con la invitación " + invitationId + " como " + role);
        return new LoginResponse(jwt.generate(AppUserDetails.from(user)), jwt.getExpirationMinutes(), UserDto.from(user));
    }

    /** Busca por huella. Cualquier fallo da el mismo mensaje para no revelar qué enlaces existieron. */
    private Invitation usable(String token) {
        if (token == null || token.length() < 20 || token.length() > 100) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID);
        }
        Invitation inv = invitations.findByTokenHash(AgentKeys.hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID));
        if (!inv.isUsable(Instant.now())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID);
        return inv;
    }

    private Company companyOf(Invitation inv) {
        return companies.findById(inv.getCompanyId()).filter(Company::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID));
    }
}
