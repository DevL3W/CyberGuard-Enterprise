package com.cyberguard.config;

import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crea la primera empresa y su administrador cuando la base de datos está vacía. */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminUser, adminPassword, companyName;

    public DataInitializer(CompanyRepository companies, AppUserRepository users, PasswordEncoder encoder,
                           @Value("${cyberguard.admin.username}") String adminUser,
                           @Value("${cyberguard.admin.password}") String adminPassword,
                           @Value("${cyberguard.admin.company}") String companyName) {
        this.companies = companies; this.users = users; this.encoder = encoder;
        this.adminUser = adminUser; this.adminPassword = adminPassword; this.companyName = companyName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (companies.count() > 0) return;
        Company company = companies.save(new Company(companyName, "CG-" + UUID.randomUUID().toString().toUpperCase()));
        users.save(new AppUser(company, adminUser.toLowerCase(), encoder.encode(adminPassword),
                "Administrador", Role.ADMIN));
        boolean porDefecto = "CambiaEsta#2026".equals(adminPassword);
        System.out.println();
        System.out.println("============================================================");
        System.out.println(" CUENTA PRINCIPAL CREADA");
        System.out.println("   Empresa:    " + companyName);
        System.out.println("   Sesión:     " + adminUser);
        System.out.println("   Contraseña: " + (porDefecto ? adminPassword : "(la definida en CYBERGUARD_ADMIN_PASSWORD)"));
        System.out.println("============================================================");
        System.out.println();
        log.warn("Se creó la empresa '{}' y el administrador '{}'. Cambie la contraseña inicial.", companyName, adminUser);
    }
}
