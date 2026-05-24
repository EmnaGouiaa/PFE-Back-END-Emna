package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;

import java.util.List;

public interface DemandeCreationCompteEntrepriseService {
    DemandeCreationCompteEntreprise createDemande(CreateDemandeCreationCompteEntrepriseRequest request);

    DemandeCreationCompteEntreprise updateDemande(Long id, CreateDemandeCreationCompteEntrepriseRequest request);

    DemandeCreationCompteEntreprise getDemandeById(Long id);

    List<DemandeCreationCompteEntreprise> getAllDemandes();

    List<DemandeCreationCompteEntreprise> getDemandesByStagiaire(Long stagiaireId);

    DemandeCreationCompteEntreprise validerParResponsableStages(Long demandeId);

    DemandeCreationCompteEntreprise refuserParResponsableStages(Long demandeId, String commentaire);
}
