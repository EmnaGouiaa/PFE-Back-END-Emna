package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncadrantProfessionnelRepository extends JpaRepository<EncadrantProfessionnel, Long> {
    Optional<EncadrantProfessionnel> findByEmail(String email);

    List<EncadrantProfessionnel> findByEntrepriseId(Long entrepriseId);

    /**
     * Returns only active (non-soft-deleted) encadrants for the given entreprise.
     * Used by getByEntrepriseId to avoid exposing soft-deleted accounts.
     */
    List<EncadrantProfessionnel> findByEntrepriseIdAndSupprimeIsFalse(Long entrepriseId);

    boolean existsByEmail(String email);
    
}
