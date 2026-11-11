package com.cyberguard.repository;

import com.cyberguard.entity.Invitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    List<Invitation> findTop100ByCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<Invitation> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Gasta un uso de forma atómica: si dos personas llegan a la vez con el último uso disponible,
     * solo una lo consigue. Devuelve 0 si la invitación ya no es válida.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Invitation i set i.uses = i.uses + 1 "
            + "where i.id = :id and i.revoked = false and i.uses < i.maxUses and i.expiresAt > :now")
    int consume(@Param("id") Long id, @Param("now") Instant now);
}
