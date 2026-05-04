package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.StatutDemande;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeCreationCompteEntrepriseRepository extends JpaRepository<DemandeCreationCompteEntreprise, Long> {

    List<DemandeCreationCompteEntreprise> findByStagiaireId(Long stagiaireId);

    List<DemandeCreationCompteEntreprise> findByStatut(StatutDemande statut);

    List<DemandeCreationCompteEntreprise> findByStatutAdmin(StatutValidation statutAdmin);

    List<DemandeCreationCompteEntreprise> findByStatutResponsableStages(StatutValidation statutResponsableStages);

    List<DemandeCreationCompteEntreprise> findByNomEntrepriseContainingIgnoreCase(String nomEntreprise);

    boolean existsByEmailEntreprise(String emailEntreprise);

    boolean existsByEmailResponsable(String emailResponsable);
}