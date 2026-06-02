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

/**
 * Contrôleur REST principal des stages (parcours stagiaire, encadrement, validations, Trello, rapports).
 * <p>
 * <strong>Domaine exposé :</strong> entité {@link Stage}, affectations, validations sujet, intégrations Trello.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/stages}
 * <p>
 * <strong>Sécurité :</strong> annotations {@code @PreAuthorize} par rôle sur les endpoints sensibles ;
 * CRUD de base sans restriction explicite au contrôleur.
 * <p>
 * <strong>Services injectés :</strong> {@link StageService}
 */
@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    /**
     * Crée un nouveau stage.
     *
     * @param request données de création du stage
     * @return {@link ResponseEntity} 201 avec l'entité {@link Stage}
     */
    @PostMapping
    public ResponseEntity<Stage> create(@RequestBody CreateStageRequest request) {
        return new ResponseEntity<>(stageService.createStage(request), HttpStatus.CREATED);
    }

    /**
     * Met à jour un stage existant.
     *
     * @param id      identifiant du stage
     * @param request champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Stage> update(@PathVariable Long id, @RequestBody CreateStageRequest request) {
        return ResponseEntity.ok(stageService.updateStage(id, request));
    }

    /**
     * Récupère un stage par identifiant.
     *
     * @param id identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Stage> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.getStageById(id));
    }

    /**
     * Liste tous les stages, avec filtre optionnel par statut métier de suivi.
     *
     * @param statutSuivi {@code NON_COMMENCE}, {@code EN_COURS}, {@code TERMINE} ou {@code REFUSE} (optionnel)
     * @return {@link ResponseEntity} 200 avec la liste des stages
     */
    @GetMapping
    public ResponseEntity<List<Stage>> getAll(
            @RequestParam(name = "statutSuivi", required = false) String statutSuivi
    ) {
        return ResponseEntity.ok(stageService.getAllStages(statutSuivi));
    }

    /**
     * Supprime un stage.
     *
     * @param id identifiant du stage
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.deleteStage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Affecte un encadrant académique à un stage.
     *
     * @param stageId     identifiant du stage
     * @param encadrantId identifiant de l'encadrant académique
     * @return {@link ResponseEntity} 200 avec {@link Stage} mis à jour
     */
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

    /**
     * Affecte un encadrant professionnel à un stage.
     *
     * @param stageId     identifiant du stage
     * @param encadrantId identifiant de l'encadrant professionnel
     * @return {@link ResponseEntity} 200 avec {@link Stage} mis à jour
     */
    @PutMapping("/{stageId}/affecter-encadrant-professionnel/{encadrantId}")
    public ResponseEntity<Stage> affecterEncadrantProfessionnel(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(
                stageService.affecterEncadrantProfessionnel(stageId, encadrantId)
        );
    }

    /**
     * Valide le stage côté entreprise (responsable entreprise connecté).
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @PutMapping("/{stageId}/valider-entreprise")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<Stage> validerStageParEntreprise(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerStageParEntreprise(stageId));
    }

    /**
     * Valide le stage côté responsable (université ou entreprise selon le service).
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @PutMapping("/{stageId}/valider-responsable")
    public ResponseEntity<Stage> validerStageParResponsable(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerStageParResponsable(stageId));
    }

    /**
     * Liste les stages d'un stagiaire donné.
     *
     * @param stagiaireId identifiant du stagiaire
     * @return {@link ResponseEntity} 200 avec la liste des stages
     */
    @GetMapping("/stagiaire/{stagiaireId}")
    public ResponseEntity<List<Stage>> getByStagiaire(@PathVariable Long stagiaireId) {
        return ResponseEntity.ok(stageService.getStagesByStagiaire(stagiaireId));
    }

    /**
     * Liste les stages du stagiaire actuellement connecté.
     *
     * @return {@link ResponseEntity} 200 avec les stages du stagiaire authentifié
     */
    @GetMapping("/mes-stages")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<Stage>> getMesStages() {
        return ResponseEntity.ok(stageService.getStagesPourStagiaireAuthentifie());
    }

    /**
     * Retourne le stage courant du stagiaire connecté.
     *
     * @return {@link ResponseEntity} 200 avec le {@link Stage} actif
     */
    @GetMapping("/mon-stage")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<Stage> getMonStage() {
        return ResponseEntity.ok(stageService.getStageCourantPourStagiaireAuthentifie());
    }

    /**
     * Liste les stages de l'entreprise du responsable connecté.
     *
     * @return {@link ResponseEntity} 200 avec les stages du périmètre entreprise
     */
    @GetMapping("/mon-entreprise")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<List<Stage>> getStagesPourMonEntreprise() {
        return ResponseEntity.ok(stageService.getStagesPourResponsableEntrepriseAuthentifie());
    }

    /**
     * Liste les stages hébergés par une entreprise.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des stages
     */
    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<Stage>> getByEntreprise(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(stageService.getStagesByEntreprise(entrepriseId));
    }

    /**
     * Liste les stages supervisés par un encadrant académique (vue RSS / admin).
     *
     * @param encadrantId identifiant de l'encadrant académique
     * @return {@link ResponseEntity} 200 avec la liste des stages
     */
    @GetMapping("/encadrant-academique/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<Stage>> getByEncadrantAcademique(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(stageService.getStagesByEncadrantAcademique(encadrantId));
    }

    /**
     * Liste les stages supervisés par un encadrant professionnel (vue RSS / admin).
     *
     * @param encadrantId identifiant de l'encadrant professionnel
     * @return {@link ResponseEntity} 200 avec la liste des stages
     */
    @GetMapping("/encadrant-professionnel/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<Stage>> getByEncadrantProfessionnel(@PathVariable Long encadrantId) {
        return ResponseEntity.ok(stageService.getStagesByEncadrantProfessionnel(encadrantId));
    }

    /**
     * Liste les stages de l'encadrant académique connecté.
     *
     * @return {@link ResponseEntity} 200 avec les stages de l'encadrant authentifié
     */
    @GetMapping("/encadrant-academique-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<List<Stage>> getPourEncadrantAcademiqueConnecte() {
        return ResponseEntity.ok(stageService.getStagesPourEncadrantAcademiqueAuthentifie());
    }

    /**
     * Liste les stages de l'encadrant professionnel connecté.
     *
     * @return {@link ResponseEntity} 200 avec les stages de l'encadrant authentifié
     */
    @GetMapping("/encadrant-professionnel-connecte")
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<List<Stage>> getPourEncadrantProfessionnelConnecte() {
        return ResponseEntity.ok(stageService.getStagesPourEncadrantProfessionnelAuthentifie());
    }

    /**
     * Déclenche manuellement le passage au statut « en cours » des stages éligibles.
     * <p>Réservé à {@code RESPONSABLE_STAGE} et {@code ADMINISTRATEUR} ; complète le job cron nocturne.
     *
     * @return {@link ResponseEntity} 200 avec {@code {"stagesDeclenchés": n}}
     */
    @PostMapping("/declencher-eligibles")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Integer>> declencherStagesEligibles() {
        int declenches = stageService.declencherStagesEligibles();
        return ResponseEntity.ok(Map.of("stagesDeclenchés", declenches));
    }

    /**
     * Crée un stage à partir d'une offre validée et d'un stagiaire.
     *
     * @param offreId     identifiant de l'offre
     * @param stagiaireId identifiant du stagiaire
     * @return {@link ResponseEntity} 201 avec le {@link Stage} créé
     */
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

    /**
     * Valide le sujet de stage par un encadrant académique (identifiant explicite).
     *
     * @param stageId     identifiant du stage
     * @param encadrantId identifiant de l'encadrant validateur
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @PutMapping("/{stageId}/valider-sujet/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Stage> validerSujetParEncadrant(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(stageService.validerSujetParEncadrantAcademique(stageId, encadrantId));
    }

    /**
     * Refuse le sujet de stage par un encadrant académique (identifiant explicite).
     *
     * @param stageId     identifiant du stage
     * @param encadrantId identifiant de l'encadrant
     * @return {@link ResponseEntity} 204 sans corps (stage supprimé automatiquement)
     */
    @PutMapping("/{stageId}/refuser-sujet/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Void> refuserSujetParEncadrant(
            @PathVariable Long stageId,
            @PathVariable Long encadrantId
    ) {
        stageService.refuserSujetParEncadrantAcademique(stageId, encadrantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valide le sujet par l'encadrant académique connecté.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link Stage}
     */
    @PutMapping("/{stageId}/valider-sujet-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<Stage> validerSujetParEncadrantConnecte(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.validerSujetParEncadrantAcademiqueAuthentifie(stageId));
    }

    /**
     * Refuse le sujet par l'encadrant académique connecté.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 204 sans corps (stage supprimé automatiquement)
     */
    @PutMapping("/{stageId}/refuser-sujet-connecte")
    @PreAuthorize("hasRole('ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<Void> refuserSujetParEncadrantConnecte(@PathVariable Long stageId) {
        stageService.refuserSujetParEncadrantAcademiqueAuthentifie(stageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retourne un résumé des cartes / colonnes Trello liées au stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec map de données Trello
     */
    @GetMapping("/{stageId:\\d+}/resume-trello")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getResumeTrelloStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.getResumeTrelloStage(stageId));
    }

    /**
     * Récupère ou crée le tableau Trello associé au stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec les métadonnées du board
     */
    @GetMapping("/{stageId:\\d+}/trello-board")
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<Map<String, Object>> getOrCreateTrelloBoard(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.createTrelloBoardIfNotExists(stageId));
    }

    /**
     * Génère un rapport synthétique du stage (statistiques, jalons, etc.).
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec le contenu du rapport (structure map)
     */
    @GetMapping("/{stageId:\\d+}/generer-rapport")
    public ResponseEntity<Map<String, Object>> genererRapportStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageService.genererRapportStage(stageId));
    }

}
