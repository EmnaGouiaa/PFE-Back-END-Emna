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

/**
 * Contrôleur REST du cycle de vie des offres de stage.
 * <p>
 * <strong>Domaine exposé :</strong> création, validation universitaire, publication, affectation étudiant, fermeture.
 * <p>
 * <strong>Chemins de base :</strong> {@code /api/offres} et alias {@code /api/offres-stage}
 * <p>
 * <strong>Sécurité :</strong> rôles {@code RESPONSABLE_ENTREPRISE}, {@code RESPONSABLE_STAGE},
 * {@code ADMINISTRATEUR} selon l'étape du workflow.
 * <p>
 * <strong>Services injectés :</strong> {@link OffreStageService}
 */
@RestController
@RequestMapping({"/api/offres", "/api/offres-stage"})
@RequiredArgsConstructor
public class OffreStageController {

    private static final Logger log = LoggerFactory.getLogger(OffreStageController.class);

    private final OffreStageService offreStageService;

    /**
     * Crée une nouvelle offre de stage (brouillon ou soumise selon le service).
     *
     * @param request données de l'offre
     * @return {@link ResponseEntity} 201 avec {@link OffreStageResponse}
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_ENTREPRISE', 'RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> create(@RequestBody CreateOffreStageRequest request) {
        return new ResponseEntity<>(offreStageService.createOffre(request), HttpStatus.CREATED);
    }

    /**
     * Met à jour une offre existante.
     *
     * @param id      identifiant de l'offre
     * @param request champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link OffreStageResponse}
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_ENTREPRISE', 'RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> update(@PathVariable Long id, @RequestBody CreateOffreStageRequest request) {
        return ResponseEntity.ok(offreStageService.updateOffre(id, request));
    }

    /**
     * Liste les offres en attente de validation universitaire.
     *
     * @return {@link ResponseEntity} 200 avec les offres au statut « en attente »
     */
    @GetMapping("/en-attente-validation")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<List<OffreStageResponse>> getOffresEnAttenteValidation() {
        List<OffreStageResponse> offers = offreStageService.getOffresEnAttenteValidation();
        log.info("GET /api/offres/en-attente-validation -> {} offre(s) en attente", offers.size());
        return ResponseEntity.ok(offers);
    }

    /**
     * Récupère une offre par identifiant.
     *
     * @param id identifiant de l'offre
     * @return {@link ResponseEntity} 200 avec {@link OffreStageResponse}
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OffreStageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(offreStageService.getOffreById(id));
    }

    /**
     * Liste toutes les offres (catalogue public / filtré côté service).
     *
     * @return {@link ResponseEntity} 200 avec la liste des offres
     */
    @GetMapping
    public ResponseEntity<List<OffreStageResponse>> getAll() {
        return ResponseEntity.ok(offreStageService.getAllOffres());
    }

    /**
     * Liste toutes les offres pour l'écran de gestion universitaire (tous statuts).
     *
     * @return {@link ResponseEntity} 200 avec la liste complète visible pour le RSS
     */
    @GetMapping("/toutes")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<List<OffreStageResponse>> getToutesPourGestion() {
        List<OffreStageResponse> offers = offreStageService.getToutesOffresPourGestion();
        log.info("GET /api/offres/toutes -> {} offre(s) visible(s) pour gestion universitaire", offers.size());
        return ResponseEntity.ok(offers);
    }

    /**
     * Supprime une offre (administration uniquement).
     *
     * @param id identifiant de l'offre
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offreStageService.deleteOffre(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publie une offre validée (passage au statut visible aux stagiaires).
     *
     * @param offreId                  identifiant de l'offre
     * @param responsableEntrepriseId  identifiant du responsable qui publie
     * @return {@link ResponseEntity} 200 avec {@link OffreStageResponse}
     */
    @PutMapping("/{offreId:\\d+}/publier/{responsableEntrepriseId:\\d+}")
    public ResponseEntity<OffreStageResponse> publierOffre(
            @PathVariable Long offreId,
            @PathVariable Long responsableEntrepriseId
    ) {
        return ResponseEntity.ok(offreStageService.publierOffre(offreId, responsableEntrepriseId));
    }

    /**
     * Approuve une offre par le service des stages (corps JSON avec identifiant RSS).
     *
     * @param offreId identifiant de l'offre
     * @param request identifiant du responsable service stages
     * @return {@link ResponseEntity} 200 avec offre validée
     * @throws IllegalArgumentException si le responsable universitaire est absent du corps
     */
    @PatchMapping("/{offreId:\\d+}/approuver")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
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

    /**
     * Approuve une offre (route legacy avec identifiant RSS dans l'URL).
     *
     * @param offreId                    identifiant de l'offre
     * @param responsableServiceStagesId identifiant du RSS validateur
     * @return {@link ResponseEntity} 200 avec offre validée
     */
    @PatchMapping("/{offreId:\\d+}/approuver/{responsableServiceStagesId:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> approuverOffreLegacy(
            @PathVariable Long offreId,
            @PathVariable Long responsableServiceStagesId
    ) {
        return ResponseEntity.ok(offreStageService.validerOffre(offreId, responsableServiceStagesId));
    }

    /**
     * Valide une offre (alias PUT legacy, même logique que PATCH approuver).
     *
     * @param offreId                    identifiant de l'offre
     * @param responsableServiceStagesId identifiant du RSS
     * @return {@link ResponseEntity} 200 avec offre validée
     */
    @PutMapping("/{offreId:\\d+}/valider/{responsableServiceStagesId:\\d+}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> validerOffreLegacy(
            @PathVariable Long offreId,
            @PathVariable Long responsableServiceStagesId
    ) {
        return ResponseEntity.ok(offreStageService.validerOffre(offreId, responsableServiceStagesId));
    }

    /**
     * Refuse une offre avec motif optionnel.
     *
     * @param offreId identifiant de l'offre
     * @param request motif de refus (optionnel)
     * @return {@link ResponseEntity} 200 avec offre refusée
     */
    @PatchMapping("/{offreId:\\d+}/refuser")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> refuserOffre(
            @PathVariable Long offreId,
            @RequestBody(required = false) RefuserOffreStageRequest request
    ) {
        return ResponseEntity.ok(
                offreStageService.refuserOffre(offreId, request != null ? request.getMotifRefus() : null)
        );
    }

    /**
     * Refuse une offre sans motif (route PUT legacy).
     *
     * @param offreId identifiant de l'offre
     * @return {@link ResponseEntity} 200 avec offre refusée
     */
    @PutMapping("/{offreId:\\d+}/refuser")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    public ResponseEntity<OffreStageResponse> refuserOffreLegacy(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.refuserOffre(offreId, null));
    }

    /**
     * Affecte un étudiant (stagiaire) à une offre par e-mail.
     * <p>Règle : l'e-mail doit correspondre à un stagiaire inscrit ; l'offre doit être publiée.
     *
     * @param offreId identifiant de l'offre
     * @param request e-mail de l'étudiant
     * @return {@link ResponseEntity} 201 avec {@link AffecterEtudiantOffreResponse}
     * @throws IllegalArgumentException si l'e-mail est vide
     */
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

    /**
     * Annule l'affectation d'un étudiant sur une offre.
     *
     * @param offreId identifiant de l'offre
     * @return {@link ResponseEntity} 200 avec {@link AnnulerAffectationOffreResponse}
     */
    @PatchMapping("/{offreId:\\d+}/annuler-affectation")
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    public ResponseEntity<AnnulerAffectationOffreResponse> annulerAffectation(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.annulerAffectation(offreId));
    }

    /**
     * Ferme une offre (plus de candidatures acceptées).
     *
     * @param offreId identifiant de l'offre
     * @return {@link ResponseEntity} 200 avec {@link OffreStageResponse}
     */
    @PutMapping("/{offreId:\\d+}/fermer")
    public ResponseEntity<OffreStageResponse> fermerOffre(@PathVariable Long offreId) {
        return ResponseEntity.ok(offreStageService.fermerOffre(offreId));
    }

    /**
     * Liste les offres d'une entreprise donnée.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des offres
     */
    @GetMapping("/entreprise/{entrepriseId:\\d+}")
    public ResponseEntity<List<OffreStageResponse>> getByEntreprise(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(offreStageService.getOffresByEntreprise(entrepriseId));
    }

    /**
     * Liste les offres ouvertes aux candidatures stagiaires.
     *
     * @return {@link ResponseEntity} 200 avec les offres publiées et non pourvues
     */
    @GetMapping("/ouvertes")
    public ResponseEntity<List<OffreStageResponse>> getOffresOuvertes() {
        List<OffreStageResponse> offers = offreStageService.getOffresOuvertes();
        log.info("GET /api/offres/ouvertes -> {} offre(s) visible(s) pour les stagiaires", offers.size());
        return ResponseEntity.ok(offers);
    }

}
