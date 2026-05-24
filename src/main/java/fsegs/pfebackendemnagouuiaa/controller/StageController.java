package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.services.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @PostMapping
    public ResponseEntity<Stage> create(@RequestBody CreateStageRequest request) {
        return new ResponseEntity<>(stageService.createStage(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Stage> update(@PathVariable Long id, @RequestBody CreateStageRequest request) {
        return ResponseEntity.ok(stageService.updateStage(id, request));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Stage> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.getStageById(id));
    }

    @GetMapping
    public ResponseEntity<List<Stage>> getAll() {
        return ResponseEntity.ok(stageService.getAllStages());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.deleteStage(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{stageId}/affecter-encadrant-academique/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Stage> affecterEncadrantAcademique(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(
                stageService.affecterEncadrantAcademique(stageId, encadrantId)
        );
    }

    @PutMapping("/{stageId}/affecter-encadrant-professionnel/{encadrantId}")
    public ResponseEntity<Stage> affecterEncadrantProfessionnel(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(
                stageService.affecterEncadrantProfessionnel(stageId, encadrantId)
        );
    }

    @PutMapping("/{stageId}/valider-entreprise")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<Stage> validerStageParEntreprise(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerStageParEntreprise(stageId));
    }

    @PutMapping("/{stageId}/valider-responsable")
    public ResponseEntity<Stage> validerStageParResponsable(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerStageParResponsable(stageId));
    }

    @GetMapping("/stagiaire/{stagiaireId}")
    public ResponseEntity<List<Stage>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(stageService.getStagesByStagiaire(stagiaireId));
    }

    @GetMapping("/mes-stages")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<Stage>> getMesStages() {
        return ResponseEntity.ok(stageService.getStagesPourStagiaireAuthentifie());
    }

    @GetMapping("/mon-stage")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<Stage> getMonStage() {
        return ResponseEntity.ok(stageService.getStageCourantPourStagiaireAuthentifie());
    }

    @GetMapping("/mon-entreprise")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<List<Stage>> getStagesPourMonEntreprise() {
        return ResponseEntity.ok(stageService.getStagesPourResponsableEntrepriseAuthentifie());
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<Stage>> getByEntreprise(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(stageService.getStagesByEntreprise(entrepriseId));
    }

    @GetMapping("/encadrant-academique/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<Stage>> getByEncadrantAcademique(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(stageService.getStagesByEncadrantAcademique(encadrantId));
    }

    @GetMapping("/encadrant-professionnel/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<Stage>> getByEncadrantProfessionnel(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(stageService.getStagesByEncadrantProfessionnel(encadrantId));
    }

    @GetMapping("/encadrant-academique-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<List<Stage>> getPourEncadrantAcademiqueConnecte() {
        return ResponseEntity.ok(stageService.getStagesPourEncadrantAcademiqueAuthentifie());
    }

    @GetMapping("/encadrant-professionnel-connecte")
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<List<Stage>> getPourEncadrantProfessionnelConnecte() {
        return ResponseEntity.ok(stageService.getStagesPourEncadrantProfessionnelAuthentifie());
    }

    /**
     * Déclenche manuellement la vérification des stages éligibles au démarrage.
     * Réservé aux rôles RESPONSABLE_STAGE et ADMINISTRATEUR.
     * Utile pour les tests ou pour forcer le déclenchement hors du cron nocturne.
     */
    @PostMapping("/declencher-eligibles")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Integer>> declencherStagesEligibles() {
        int declenches = stageService.declencherStagesEligibles();
        return ResponseEntity.ok(Map.of("stagesDeclenchés", declenches));
    }

    @PostMapping("/creer-depuis-offre/{offreId}/stagiaire/{stagiaireId}")
    public ResponseEntity<Stage> creerDepuisOffre(
            @PathVariable Long offreId,
            @PathVariable Long stagiaireId
    ) {
        return new ResponseEntity<>(
                stageService.creerStageDepuisOffre(offreId, stagiaireId),
                HttpStatus.CREATED
        );
    }
    @PutMapping("/{stageId}/valider-sujet/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Stage> validerSujetParEncadrant(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(stageService.validerSujetParEncadrantAcademique(stageId, encadrantId));
    }

    @PutMapping("/{stageId}/refuser-sujet/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Stage> refuserSujetParEncadrant(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(stageService.refuserSujetParEncadrantAcademique(stageId, encadrantId));
    }

    @PutMapping("/{stageId}/valider-sujet-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<Stage> validerSujetParEncadrantConnecte(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerSujetParEncadrantAcademiqueAuthentifie(stageId));
    }

    @PutMapping("/{stageId}/refuser-sujet-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<Stage> refuserSujetParEncadrantConnecte(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.refuserSujetParEncadrantAcademiqueAuthentifie(stageId));
    }
    @GetMapping("/{stageId:\\d+}/resume-trello")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<Map<String, Object>> getResumeTrelloStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.getResumeTrelloStage(stageId));
    }

    @GetMapping("/{stageId:\\d+}/trello-board")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<Map<String, Object>> getOrCreateTrelloBoard(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.createTrelloBoardIfNotExists(stageId));
    }

    @GetMapping("/{stageId:\\d+}/generer-rapport")
    public ResponseEntity<Map<String, Object>> genererRapportStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.genererRapportStage(stageId));
    }

}
