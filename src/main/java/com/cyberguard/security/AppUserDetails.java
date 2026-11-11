package com.cyberguard.security;

import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {
    private final Long id;
    private final Long companyId;
    private final String companyName;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final boolean active;
    private final Instant passwordChangedAt;

    public AppUserDetails(Long id, Long companyId, String companyName, String username,
                          String passwordHash, String fullName, Role role, boolean active) {
        this(id, companyId, companyName, username, passwordHash, fullName, role, active, null);
    }

    public AppUserDetails(Long id, Long companyId, String companyName, String username,
                          String passwordHash, String fullName, Role role, boolean active,
                          Instant passwordChangedAt) {
        this.id = id; this.companyId = companyId; this.companyName = companyName;
        this.username = username; this.passwordHash = passwordHash; this.fullName = fullName;
        this.role = role; this.active = active; this.passwordChangedAt = passwordChangedAt;
    }

    public static AppUserDetails from(AppUser u) {
        return new AppUserDetails(u.getId(), u.getCompany().getId(), u.getCompany().getName(),
                u.getUsername(), u.getPasswordHash(), u.getFullName(), u.getRole(),
                u.isEnabled() && u.getCompany().isActive(), u.getPasswordChangedAt());
    }

    /**
     * Un token emitido antes del último cambio de contraseña deja de valer. La fecha del token tiene
     * precisión de segundos, por eso se compara contra el cambio truncado al segundo.
     */
    public boolean acceptsTokenIssuedAt(Instant issuedAt) {
        return passwordChangedAt == null || !issuedAt.isBefore(passwordChangedAt.truncatedTo(ChronoUnit.SECONDS));
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return active; }
}
