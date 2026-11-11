package com.cyberguard.config;

import com.cyberguard.security.AgentAuthFilter;
import com.cyberguard.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // Los filtros solo deben ejecutarse dentro de la cadena de Spring Security, no como filtros globales
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false);
        return r;
    }

    @Bean
    FilterRegistrationBean<AgentAuthFilter> agentFilterRegistration(AgentAuthFilter filter) {
        FilterRegistrationBean<AgentAuthFilter> r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false);
        return r;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtFilter,
                                            AgentAuthFilter agentFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // API sin estado con token Bearer, sin cookies de sesión
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/guest", "/api/auth/register", "/api/auth/config").permitAll()
                // Invitaciones: quien recibe el enlace o el QR todavía no tiene sesión
                .requestMatchers("/api/join/**").permitAll()
                // Agentes: solo su propia API, con clave de dispositivo
                .requestMatchers("/api/agent/**").hasRole("AGENT")
                // Administrador: sesiones, invitaciones, empresa/licencia y auditoría
                .requestMatchers("/api/users/**", "/api/company/**", "/api/audit/**", "/api/invitations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/devices/**").hasRole("ADMIN")
                // Administrador y analista: registrar y modificar
                .requestMatchers(HttpMethod.POST, "/api/devices/**", "/api/alerts/**",
                        "/api/vulnerabilities/**", "/api/incidents/**").hasAnyRole("ADMIN", "ANALISTA")
                .requestMatchers(HttpMethod.PUT, "/api/devices/**").hasAnyRole("ADMIN", "ANALISTA")
                .requestMatchers(HttpMethod.PATCH, "/api/alerts/**", "/api/vulnerabilities/**",
                        "/api/incidents/**").hasAnyRole("ADMIN", "ANALISTA")
                // Cualquier sesión de usuario (incluido solo consulta): leer
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "ANALISTA", "LECTOR")
                .anyRequest().permitAll()) // páginas estáticas; los datos siempre pasan por /api
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(h -> h.frameOptions(f -> f.deny()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(agentFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
