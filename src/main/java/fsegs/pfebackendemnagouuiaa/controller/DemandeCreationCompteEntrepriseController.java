package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.dto.RejectDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.services.DemandeCreationCompteEntrepriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-stage")
@RequiredArgsConstructor
public class DemandeCreationCompteEntrepriseController {

    private final DemandeCreationCompteEntrepriseService demandeService;

    @PreAuthorize("hasRole('STAGIAIRE')")
    @PostMapping
    public ResponseEntity<DemandeCreationCompteEntreprise> create(
            @RequestBody CreateDemandeCreationCompteEntrepriseRequest request
    ) {
        return new ResponseEntity<>(demandeService.createDemande(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('STAGIAIRE')")
    @PutMapping("/{id}")
    public ResponseEntity<DemandeCreationCompteEntreprise> update(
            @PathVariable Long id,
            @RequestBody CreateDemandeCreationCompteEntrepriseRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateDemande(id, request));
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping("/{id}")
    public ResponseEntity<DemandeCreationCompteEntreprise> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getDemandeById(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getAll() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping("/stagiaire/{stagiaireId}")
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(demandeService.getDemandesByStagiaire(stagiaireId));
    }

    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    @PutMapping("/{demandeId}/valider-responsable-stages")
    public ResponseEntity<DemandeCreationCompteEntreprise> validerParResponsableStages(
            @PathVariable Long demandeId
    ) {
        return ResponseEntity.ok(demandeService.validerParResponsableStages(demandeId));
    }

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
