package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;

import java.util.List;

public interface DemandeCreationCompteEntrepriseService {
    DemandeCreationCompteEntreprise createDemande(CreateDemandeCreationCompteEntrepriseRequest request);

    DemandeCreationCompteEntreprise updateDemande(Long id, CreateDemandeCreationCompteEntrepriseRequest request);

    DemandeCreationCompteEntreprise getDemandeById(Long id);

    List<DemandeCreationCompteEntreprise> getAllDemandes();

    void deleteDemande(Long id);

    List<DemandeCreationCompteEntreprise> getDemandesByStagiaire(Long stagiaireId);

    DemandeCreationCompteEntreprise validerParAdmin(Long demandeId, Long adminId);

    DemandeCreationCompteEntreprise refuserParAdmin(Long demandeId, Long adminId, String commentaire);

    DemandeCreationCompteEntreprise validerParEncadrantAcademique(Long demandeId, Long encadrantId);

    DemandeCreationCompteEntreprise refuserParEncadrantAcademique(Long demandeId, Long encadrantId);

    DemandeCreationCompteEntreprise validerParResponsableStages(Long demandeId);

    DemandeCreationCompteEntreprise refuserParResponsableStages(Long demandeId, String commentaire);
}
