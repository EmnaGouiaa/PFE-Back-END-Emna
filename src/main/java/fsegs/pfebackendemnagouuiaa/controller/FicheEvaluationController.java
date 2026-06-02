package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.services.FicheEvaluationService;
import fsegs.pfebackendemnagouuiaa.services.StageDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des fiches d'évaluation de fin de stage.
 * <p>
 * <strong>Domaine exposé :</strong> fiche, notes par critère, signatures, export PDF.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/fiches-evaluation}
 * <p>
 * <strong>Sécurité :</strong> saisie entreprise ({@code ENCADRANT_PROFESSIONNEL}, {@code RESPONSABLE_ENTREPRISE}) ;
 * lecture élargie aux acteurs du stage.
 * <p>
 * <strong>Services injectés :</strong>
 * <ul>
 *   <li>{@link FicheEvaluationService} — persistance et règles métier</li>
 *   <li>{@link StageDocumentService} — export PDF (politique d'accès centralisée)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/fiches-evaluation")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FicheEvaluationController {

    private final FicheEvaluationService ficheEvaluationService;
    private final StageDocumentService stageDocumentService;

    /**
     * Crée une fiche d'évaluation pour un stage.
     *
     * @param dto données initiales de la fiche
     * @return {@link ResponseEntity} 201 avec {@link FicheEvaluationDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PostMapping
    public ResponseEntity<FicheEvaluationDto> create(@RequestBody FicheEvaluationDto dto) {
        return new ResponseEntity<>(ficheEvaluationService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère une fiche par identifiant.
     *
     * @param id identifiant de la fiche
     * @return {@link ResponseEntity} 200 avec {@link FicheEvaluationDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<FicheEvaluationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ficheEvaluationService.getById(id));
    }

    /**
     * Liste toutes les fiches (pilotage universitaire).
     *
     * @return {@link ResponseEntity} 200 avec la liste des fiches
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping
    public ResponseEntity<List<FicheEvaluationDto>> getAll() {
        return ResponseEntity.ok(ficheEvaluationService.getAll());
    }

    /**
     * Récupère la fiche liée à un stage, ou 204 si absente.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 ou 204
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<FicheEvaluationDto> getByStageId(@PathVariable Long stageId) {
        return ficheEvaluationService.findByStageIdIfPresent(stageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Liste les fiches associées à une réunion finale.
     *
     * @param reunionFinaleId identifiant de la réunion finale
     * @return {@link ResponseEntity} 200 avec la liste des fiches
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/reunion-finale/{reunionFinaleId}")
    public ResponseEntity<List<FicheEvaluationDto>> getByReunionFinaleId(@PathVariable Long reunionFinaleId) {
        return ResponseEntity.ok(ficheEvaluationService.getByReunionFinaleId(reunionFinaleId));
    }

    /**
     * Met à jour une fiche d'évaluation.
     *
     * @param id  identifiant de la fiche
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link FicheEvaluationDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<FicheEvaluationDto> update(@PathVariable Long id,
                                                     @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.update(id, dto));
    }

    /**
     * Enregistre la signature d'un utilisateur sur la fiche.
     *
     * @param id     identifiant de la fiche
     * @param userId identifiant de l'utilisateur signataire
     * @return {@link ResponseEntity} 200 avec fiche signée
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/signer/{userId}")
    public ResponseEntity<FicheEvaluationDto> signerFiche(@PathVariable Long id,
                                                          @PathVariable Long userId) {
        return ResponseEntity.ok(ficheEvaluationService.signerFiche(id, userId));
    }

    /**
     * Remplit la partie réservée à l'encadrant professionnel.
     *
     * @param id     identifiant de la fiche
     * @param userId identifiant de l'encadrant professionnel
     * @param dto    contenu de sa section
     * @return {@link ResponseEntity} 200 avec {@link FicheEvaluationDto}
     */
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PatchMapping("/{id}/encadrant-pro/{userId}")
    public ResponseEntity<FicheEvaluationDto> remplirPartieEncadrantPro(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.remplirPartieEncadrantProfessionnel(id, userId, dto));
    }

    /**
     * Remplit la partie réservée au responsable entreprise.
     *
     * @param id     identifiant de la fiche
     * @param userId identifiant du responsable entreprise
     * @param dto    contenu de sa section
     * @return {@link ResponseEntity} 200 avec {@link FicheEvaluationDto}
     */
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/responsable-entreprise/{userId}")
    public ResponseEntity<FicheEvaluationDto> remplirPartieResponsableEntreprise(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.remplirPartieResponsableEntreprise(id, userId, dto));
    }

    /**
     * Enregistre les notes saisies par l'encadrant professionnel.
     *
     * @param id     identifiant de la fiche
     * @param userId identifiant de l'encadrant
     * @param notes  liste des notes par critère
     * @return {@link ResponseEntity} 200 avec fiche mise à jour
     */
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}/notes/{userId}")
    public ResponseEntity<FicheEvaluationDto> enregistrerNotes(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody List<NoteAttribueeDto> notes) {
        return ResponseEntity.ok(ficheEvaluationService.enregistrerNotesEncadrantProfessionnel(id, userId, notes));
    }

    /**
     * Enregistre les notes saisies par le responsable entreprise.
     *
     * @param id     identifiant de la fiche
     * @param userId identifiant du responsable
     * @param notes  liste des notes par critère
     * @return {@link ResponseEntity} 200 avec fiche mise à jour
     */
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}/notes-re/{userId}")
    public ResponseEntity<FicheEvaluationDto> enregistrerNotesRE(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody List<NoteAttribueeDto> notes) {
        return ResponseEntity.ok(ficheEvaluationService.enregistrerNotesResponsableEntreprise(id, userId, notes));
    }

    /**
     * Télécharge le PDF de la fiche par identifiant de fiche.
     *
     * @param id identifiant de la fiche
     * @return {@link ResponseEntity} 200 PDF, 500 en cas d'erreur de génération
     * @throws IllegalStateException si la fiche n'est pas prête pour l'export (relancée)
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> telechargerPdfParId(@PathVariable Long id) {
        FicheEvaluationDto fiche = ficheEvaluationService.getById(id);
        if (fiche.getStageId() == null) {
            return ResponseEntity.badRequest().build();
        }
        byte[] pdf = stageDocumentService.getEvaluationPdf(fiche.getStageId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"fiche-evaluation-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Télécharge le PDF de la fiche à partir de l'identifiant du stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 PDF, 500 en cas d'erreur de génération
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}/pdf")
    public ResponseEntity<byte[]> telechargerPdfParStage(@PathVariable Long stageId) {
        byte[] pdf = stageDocumentService.getEvaluationPdf(stageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"fiche-evaluation-stage-" + stageId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
