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

    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES')")
    @GetMapping("/{id}")
    public ResponseEntity<DemandeCreationCompteEntreprise> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getDemandeById(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES')")
    @GetMapping
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getAll() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        demandeService.deleteDemande(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ADMINISTRATEUR','RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES')")
    @GetMapping("/stagiaire/{stagiaireId}")
    public ResponseEntity<List<DemandeCreationCompteEntreprise>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(demandeService.getDemandesByStagiaire(stagiaireId));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{demandeId}/valider-admin/{adminId}")
    public ResponseEntity<DemandeCreationCompteEntreprise> validerParAdmin(
            @PathVariable Long demandeId,
            @PathVariable Long adminId
    ) {
        return ResponseEntity.ok(demandeService.validerParAdmin(demandeId, adminId));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{demandeId}/refuser-admin/{adminId}")
    public ResponseEntity<DemandeCreationCompteEntreprise> refuserParAdmin(
            @PathVariable Long demandeId,
            @PathVariable Long adminId,
            @RequestBody(required = false) RejectDemandeCreationCompteEntrepriseRequest request
    ) {
        return ResponseEntity.ok(demandeService.refuserParAdmin(
                demandeId,
                adminId,
                request != null ? request.getCommentaire() : null
        ));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ADMINISTRATEUR')")
    @PutMapping("/{demandeId}/valider-encadrant/{encadrantId}")
    public ResponseEntity<DemandeCreationCompteEntreprise> validerParEncadrant(
            @PathVariable Long demandeId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(demandeService.validerParEncadrantAcademique(demandeId, encadrantId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ADMINISTRATEUR')")
    @PutMapping("/{demandeId}/refuser-encadrant/{encadrantId}")
    public ResponseEntity<DemandeCreationCompteEntreprise> refuserParEncadrant(
            @PathVariable Long demandeId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(demandeService.refuserParEncadrantAcademique(demandeId, encadrantId));
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES')")
    @PutMapping("/{demandeId}/valider-responsable-stages")
    public ResponseEntity<DemandeCreationCompteEntreprise> validerParResponsableStages(
            @PathVariable Long demandeId
    ) {
        return ResponseEntity.ok(demandeService.validerParResponsableStages(demandeId));
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES')")
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
