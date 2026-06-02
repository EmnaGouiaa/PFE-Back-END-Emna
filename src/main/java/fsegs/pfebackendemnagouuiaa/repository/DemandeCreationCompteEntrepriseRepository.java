package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.StatutDemande;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeCreationCompteEntrepriseRepository extends JpaRepository<DemandeCreationCompteEntreprise, Long> {

    List<DemandeCreationCompteEntreprise> findByStagiaireId(Long stagiaireId);

    List<DemandeCreationCompteEntreprise> findByStatut(StatutDemande statut);

    List<DemandeCreationCompteEntreprise> findByStatutResponsableStages(StatutValidation statutResponsableStages);

    List<DemandeCreationCompteEntreprise> findByNomEntrepriseContainingIgnoreCase(String nomEntreprise);

    boolean existsByEmailEntreprise(String emailEntreprise);

    boolean existsByEmailResponsable(String emailResponsable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DemandeCreationCompteEntreprise d WHERE d.id = :id")
    Optional<DemandeCreationCompteEntreprise> findByIdForUpdate(@Param("id") Long id);
}
