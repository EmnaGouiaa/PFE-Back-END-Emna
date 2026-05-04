package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ApprouverOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.AffecterEtudiantOffreRequest;
import fsegs.pfebackendemnagouuiaa.dto.AffecterEtudiantOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.AnnulerAffectationOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.CreateOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.OffreStageResponse;
import fsegs.pfebackendemnagouuiaa.dto.RefuserOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.services.OffreStageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/offres", "/api/offres-stage"})
@RequiredArgsConstructor
public class OffreStageController {

    private static final Logger log = LoggerFactory.getLogger(OffreStageController.class);

    private final OffreStageService offreStageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_ENTREPRISE', 'RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> create(@RequestBody CreateOffreStageRequest request) {
        return new ResponseEntity<>(offreStageService.createOffre(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ENTREPRISE', 'RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> update(@PathVariable Long id, @RequestBody CreateOffreStageRequest request) {
        return ResponseEntity.ok(offreStageService.updateOffre(id, request));
    }

    @GetMapping("/en-attente-validation")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<List<OffreStageResponse>> getOffresEnAttenteValidation() {
        List<OffreStageResponse> offers = offreStageService.getOffresEnAttenteValidation();
        log.info("GET /api/offres/en-attente-validation -> {} offre(s) en attente", offers.size());
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OffreStageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(offreStageService.getOffreById(id));
    }

    @GetMapping
    public ResponseEntity<List<OffreStageResponse>> getAll() {
        return ResponseEntity.ok(offreStageService.getAllOffres());
    }

    @GetMapping("/toutes")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<List<OffreStageResponse>> getToutesPourGestion() {
        List<OffreStageResponse> offers = offreStageService.getToutesOffresPourGestion();
        log.info("GET /api/offres/toutes -> {} offre(s) visible(s) pour gestion universitaire", offers.size());
        return ResponseEntity.ok(offers);
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offreStageService.deleteOffre(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{offreId:\\d+}/publier/{responsableEntrepriseId:\\d+}")
    public ResponseEntity<OffreStageResponse> publierOffre(
            @PathVariable Long offreId,
            @PathVariable Long responsableEntrepriseId
    ) {
        return ResponseEntity.ok(offreStageService.publierOffre(offreId, responsableEntrepriseId));
    }

    @PatchMapping("/{offreId:\\d+}/approuver")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> approuverOffre(
            @PathVariable Long offreId,
            @RequestBody ApprouverOffreStageRequest request
    ) {
        if (request == null || request.getResponsableServiceStagesId() == null) {
            throw new IllegalArgumentException("Le responsable universitaire est requis pour approuver l'offre.");
        }

        log.info("PATCH /api/offres/{}/approuver - responsable {}", offreId, request.getResponsableServiceStagesId());

        return ResponseEntity.ok(offreStageService.validerOffre(offreId, request.getResponsableServiceStagesId()));
    }

    @PatchMapping("/{offreId:\\d+}/approuver/{responsableServiceStagesId:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> approuverOffreLegacy(
            @PathVariable Long offreId,
            @PathVariable Long responsableServiceStagesId
    ) {
        return ResponseEntity.ok(offreStageService.validerOffre(offreId, responsableServiceStagesId));
    }

    @PutMapping("/{offreId:\\d+}/valider/{responsableServiceStagesId:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> validerOffreLegacy(
            @PathVariable Long offreId,
            @PathVariable Long responsableServiceStagesId
    ) {
        return ResponseEntity.ok(offreStageService.validerOffre(offreId, responsableServiceStagesId));
    }

    @PatchMapping("/{offreId:\\d+}/refuser")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> refuserOffre(
            @PathVariable Long offreId,
            @RequestBody(required = false) RefuserOffreStageRequest request
    ) {
        return ResponseEntity.ok(
                offreStageService.refuserOffre(offreId, request != null ? request.getMotifRefus() : null)
        );
    }

    @PutMapping("/{offreId:\\d+}/refuser")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> refuserOffreLegacy(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.refuserOffre(offreId, null));
    }

    @PostMapping("/{offreId:\\d+}/affecter-etudiant")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<AffecterEtudiantOffreResponse> affecterEtudiant(
            @PathVariable Long offreId,
            @RequestBody AffecterEtudiantOffreRequest request
    ) {
        if (request == null || request.getEmailEtudiant() == null || request.getEmailEtudiant().isBlank()) {
            throw new IllegalArgumentException("L'email de l'etudiant est obligatoire.");
        }

        return new ResponseEntity<>(
                offreStageService.affecterEtudiant(offreId, request.getEmailEtudiant()),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{offreId:\\d+}/annuler-affectation")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<AnnulerAffectationOffreResponse> annulerAffectation(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.annulerAffectation(offreId));
    }

    @PutMapping("/{offreId:\\d+}/fermer")
    public ResponseEntity<OffreStageResponse> fermerOffre(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.fermerOffre(offreId));
    }

    @GetMapping("/entreprise/{entrepriseId:\\d+}")
    public ResponseEntity<List<OffreStageResponse>> getByEntreprise(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(offreStageService.getOffresByEntreprise(entrepriseId));
    }

    @GetMapping("/ouvertes")
    public ResponseEntity<List<OffreStageResponse>> getOffresOuvertes() {
        List<OffreStageResponse> offers = offreStageService.getOffresOuvertes();
        log.info("GET /api/offres/ouvertes -> {} offre(s) visible(s) pour les stagiaires", offers.size());
        return ResponseEntity.ok(offers);
    }

}
