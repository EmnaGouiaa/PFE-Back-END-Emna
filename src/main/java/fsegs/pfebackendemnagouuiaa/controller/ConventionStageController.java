package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.services.ConventionStageService;
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
 * Contrôleur REST des conventions de stage (CRUD, signatures, export PDF).
 * <p>
 * <strong>Domaine exposé :</strong> convention tripartite université / entreprise / stagiaire.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/conventions-stage}
 * <p>
 * <strong>Sécurité :</strong> signature responsable universitaire ({@code RESPONSABLE_STAGE}) ;
 * PDF accessible aux acteurs du stage ; liste globale pour pilotage universitaire.
 * <p>
 * <strong>Services injectés :</strong>
 * <ul>
 *   <li>{@link ConventionStageService} — cycle de vie et signatures</li>
 *   <li>{@link StageDocumentService} — génération PDF</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/conventions-stage")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ConventionStageController {

    private final ConventionStageService conventionStageService;
    private final StageDocumentService stageDocumentService;

    /**
     * Crée une convention de stage autonome.
     *
     * @param dto données de la convention
     * @return {@link ResponseEntity} 201 avec {@link ConventionStageDto}
     */
    @PostMapping
    public ResponseEntity<ConventionStageDto> create(@RequestBody ConventionStageDto dto) {
        return new ResponseEntity<>(conventionStageService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Crée une convention rattachée à un stage.
     *
     * @param stageId identifiant du stage
     * @param dto     contenu de la convention
     * @return {@link ResponseEntity} 201 avec {@link ConventionStageDto}
     */
    @PostMapping("/stage/{stageId}")
    public ResponseEntity<ConventionStageDto> createByStage(@PathVariable Long stageId,
                                                            @RequestBody ConventionStageDto dto) {
        return new ResponseEntity<>(conventionStageService.createByStage(stageId, dto), HttpStatus.CREATED);
    }

    /**
     * Met à jour une convention existante.
     *
     * @param id  identifiant de la convention
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConventionStageDto> update(@PathVariable Long id,
                                                     @RequestBody ConventionStageDto dto) {
        return ResponseEntity.ok(conventionStageService.update(id, dto));
    }

    /**
     * Récupère une convention par identifiant.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConventionStageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.getById(id));
    }

    /**
     * Télécharge le PDF de la convention par identifiant de convention.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec flux PDF inline
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] pdf = stageDocumentService.getConventionPdfByConventionId(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"convention-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * Récupère la convention d'un stage, ou 204 si absente.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 ou 204
     */
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<ConventionStageDto> getByStageId(@PathVariable Long stageId) {
        return conventionStageService.findConventionByStageIfPresent(stageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Liste toutes les conventions (pilotage universitaire).
     *
     * @return {@link ResponseEntity} 200 avec la liste des conventions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<ConventionStageDto>> getAll() {
        return ResponseEntity.ok(conventionStageService.getAll());
    }

    /**
     * Applique la signature du stagiaire sur la convention.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @PutMapping("/{id}/signer-stagiaire")
    public ResponseEntity<ConventionStageDto> signerParStagiaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParStagiaire(id));
    }

    /**
     * Applique la signature de l'encadrant académique.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @PutMapping("/{id}/signer-encadrant-academique")
    public ResponseEntity<ConventionStageDto> signerParEncadrantAcademique(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEncadrantAcademique(id));
    }

    /**
     * Applique la signature de l'encadrant professionnel.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @PutMapping("/{id}/signer-encadrant-professionnel")
    public ResponseEntity<ConventionStageDto> signerParEncadrantProfessionnel(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEncadrantProfessionnel(id));
    }

    /**
     * Applique la signature du représentant de l'entreprise.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec {@link ConventionStageDto}
     */
    @PutMapping("/{id}/signer-entreprise")
    public ResponseEntity<ConventionStageDto> signerParEntreprise(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEntreprise(id));
    }

    /**
     * Signature par le responsable du service des stages (université).
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec convention signée
     */
    @PutMapping("/{id}/signer-responsable")
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    public ResponseEntity<ConventionStageDto> signerParResponsable(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParResponsable(id));
    }

    /**
     * Alias PATCH pour la signature responsable universitaire (compatibilité front).
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 200 avec convention signée
     */
    @PatchMapping("/{id}/signer-responsable-universitaire")
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    public ResponseEntity<ConventionStageDto> signerParResponsableUniversitaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParResponsable(id));
    }

    /**
     * Supprime une convention.
     *
     * @param id identifiant de la convention
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conventionStageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
