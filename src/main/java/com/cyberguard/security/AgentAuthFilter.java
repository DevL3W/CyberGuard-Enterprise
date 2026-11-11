package com.cyberguard.security;

import com.cyberguard.service.AgentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica a los agentes con las cabeceras X-Device-Id y X-Agent-Key, solo en /api/agent/**. */
@Component
public class AgentAuthFilter extends OncePerRequestFilter {
    private final AgentService agents;

    public AgentAuthFilter(AgentService agents) { this.agents = agents; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/agent/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader("X-Device-Id");
        String key = request.getHeader("X-Agent-Key");
        if (id != null && key != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                agents.authenticate(Long.valueOf(id.trim()), key.trim()).ifPresent(p -> {
                    var auth = new UsernamePasswordAuthenticationToken(p, null,
                            List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (NumberFormatException ignored) {
                // ID no numérico: sin autenticar
            }
        }
        chain.doFilter(request, response);
    }
}
