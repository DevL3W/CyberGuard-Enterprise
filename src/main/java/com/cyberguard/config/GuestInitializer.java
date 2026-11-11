package com.cyberguard.config;

import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.service.GuestService;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea la cuenta de invitado (rol LECTOR) en la primera empresa si el modo invitado está activo.
 * Su contraseña es aleatoria y no se muestra: solo se entra con el botón "Entrar solo para ver".
 */
@Component
@Order(2)
public class GuestInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GuestInitializer.class);

    private final GuestService guest;
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public GuestInitializer(GuestService guest, CompanyRepository companies, AppUserRepository users,
                            PasswordEncoder encoder) {
        this.guest = guest; this.companies = companies; this.users = users; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!guest.isEnabled()) return;
        var existing = users.findByUsername(guest.getUsername());
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.LECTOR) {
                log.warn("La sesión '{}' ya existe y no es de solo consulta: el modo invitado no funcionará hasta que use otro nombre (cyberguard.guest.username).", guest.getUsername());
            }
            return;
        }
        Company company = companies.findAll().stream().min(Comparator.comparing(Company::getId)).orElse(null);
        if (company == null) return;
        users.save(new AppUser(company, guest.getUsername(), encoder.encode(UUID.randomUUID().toString()),
                "Invitado", Role.LECTOR));
        log.info("Modo invitado activo: sesión '{}' (solo consulta) en la empresa '{}'.", guest.getUsername(), company.getName());
    }
}
