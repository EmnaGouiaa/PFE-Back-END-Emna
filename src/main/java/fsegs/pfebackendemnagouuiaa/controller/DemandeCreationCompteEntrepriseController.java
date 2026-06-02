package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.dto.RejectDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.services.DemandeCreationCompteEntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des demandes de création de compte entreprise (stagiaire en stage hors plateforme).
 * <p>
 * <strong>Domaine exposé :</strong> workflow de demande pour qu'une entreprise non référencée soit intégrée.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/demandes-stage}
 * <p>
 * <strong>Sécurité :</strong> {@code STAGIAIRE} pour création/modification ;
 * {@code RESPONSABLE_STAGE} pour validation/refus ; lecture partagée selon les endpoints.
 * <p>
 * <strong>Services injectés :</strong> {@link DemandeCreationCompteEntrepriseService}
 */
@RestController
@RequestMapping("/api/demandes-stage")
@RequiredArgsConstructor
public class DemandeCreationCompteEntrepriseController {

    private final DemandeCreationCompteEntrepriseService demandeService;

    /**
     * Soumet une nouvelle demande de création de compte entreprise.
     *
     * @param request informations entreprise et stage
     * @return {@link ResponseEntity} 201 avec l'entité {@link DemandeCreationCompteEntreprise}
     */
    @PreAuthorize("hasRole('STAGIAIRE')")
    @PostMapping
    public ResponseEntity<DemandeCreationCompteEntreprise> create(
            @Valid @RequestBody CreateDemandeCreationCompteEntrepriseRequest request
    ) {
        return new ResponseEntity<>(demandeService.createDemande(request), HttpStatus.CREATED);
    }

    /**
     * Met à jour une demande existante (tant qu'elle est modifiable).
     *
     * @param id      identifiant de la demande
     * @param request champs modifiés
     * @return {@link ResponseEntity} 200 avec la demande mise à jour
     */
    @PreAuthorize("hasRole('STAGIAIRE')")
    @PutMapping("/{id}")
    public ResponseEntity<DemandeCreationCompteEntreprise> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateDemandeCreationCompteEntrepriseRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateDemande(id, request));
    }

    /**
     * Récupère une demande par identifiant.
     *
     * @param id identifiant de la demande
     * @return {@link ResponseEntity} 200 avec la demande
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping("/{id}")
    public ResponseEntity<DemandeCreationCompteEntreprise> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getDemandeById(id));
    }

    /**
     * Liste toutes les demandes (pilotage universitaire).
     *
     * @return {@link ResponseEntity} 200 avec la liste des demandes
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getAll() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    /**
     * Liste les demandes d'un stagiaire donné.
     *
     * @param stagiaireId identifiant du stagiaire
     * @return {@link ResponseEntity} 200 avec la liste des demandes du stagiaire
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping("/stagiaire/{stagiaireId}")
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(demandeService.getDemandesByStagiaire(stagiaireId));
    }

    /**
     * Valide une demande par le responsable du service des stages.
     * <p>Règle métier : déclenche la création du compte entreprise côté service.
     *
     * @param demandeId identifiant de la demande
     * @return {@link ResponseEntity} 200 avec la demande validée
     */
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    @PutMapping("/{demandeId}/valider-responsable-stages")
    public ResponseEntity<DemandeCreationCompteEntreprise> validerParResponsableStages(
            @PathVariable Long demandeId
    ) {
        return ResponseEntity.ok(demandeService.validerParResponsableStages(demandeId));
    }

    /**
     * Refuse une demande par le responsable du service des stages.
     *
     * @param demandeId identifiant de la demande
     * @param request   commentaire de refus (optionnel)
     * @return {@link ResponseEntity} 200 avec la demande refusée
     */
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    @PutMapping("/{demandeId}/refuser-responsable-stages")
    public ResponseEntity<DemandeCreationCompteEntreprise> refuserParResponsableStages(
            @PathVariable Long demandeId,
            @RequestBody(required = false) RejectDemandeCreationCompteEntrepriseRequest request
    ) {
        return ResponseEntity.ok(demandeService.refuserParResponsableStages(
                demandeId,
                request != null ? request.getCommentaire() : null
        ));
    }
}
