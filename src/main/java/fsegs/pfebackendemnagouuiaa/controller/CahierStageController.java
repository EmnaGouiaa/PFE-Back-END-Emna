package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.SignerCahierRequest;
import fsegs.pfebackendemnagouuiaa.services.CahierStageContenuService;
import fsegs.pfebackendemnagouuiaa.services.CahierStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST du cahier de stage (journal de bord et signatures multi-acteurs).
 * <p>
 * <strong>Domaine exposé :</strong> cahier de stage, signatures stagiaire / encadrants / responsable entreprise.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/cahiers-stage}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize} ; contrôle global Spring Security.
 * <p>
 * <strong>Services injectés :</strong> {@link CahierStageService}
 */
@RestController
@RequestMapping("/api/cahiers-stage")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CahierStageController {

    private final CahierStageService cahierStageService;
    private final CahierStageContenuService cahierStageContenuService;

    /**
     * Crée un cahier de stage autonome.
     *
     * @param dto données du cahier
     * @return {@link ResponseEntity} 201 avec {@link CahierStageDto}
     */
    @PostMapping
    public ResponseEntity<CahierStageDto> create(@RequestBody CahierStageDto dto) {
        return new ResponseEntity<>(cahierStageService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Crée un cahier de stage rattaché à un stage existant.
     *
     * @param stageId identifiant du stage
     * @param dto     contenu initial du cahier
     * @return {@link ResponseEntity} 201 avec {@link CahierStageDto}
     */
    @PostMapping("/stage/{stageId}")
    public ResponseEntity<CahierStageDto> createByStage(@PathVariable Long stageId,
                                                        @RequestBody CahierStageDto dto) {
        return new ResponseEntity<>(cahierStageService.createByStage(stageId, dto), HttpStatus.CREATED);
    }

    /**
     * Met à jour un cahier de stage.
     *
     * @param id  identifiant du cahier
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CahierStageDto> update(@PathVariable Long id,
                                                 @RequestBody CahierStageDto dto) {
        return ResponseEntity.ok(cahierStageService.update(id, dto));
    }

    /**
     * Récupère un cahier par identifiant.
     *
     * @param id identifiant du cahier
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CahierStageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.getById(id));
    }

    /**
     * Récupère le cahier lié à un stage, ou 204 si aucun cahier n'existe encore.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec le DTO, ou 204 sans corps
     */
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<CahierStageDto> getByStageId(@PathVariable Long stageId) {
        return cahierStageService.findByStageIdIfPresent(stageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Contenu complet du cahier (informations, signatures, réunions, Trello, absences, état PDF).
     */
    @GetMapping("/stage/{stageId}/contenu")
    public ResponseEntity<CahierStageContenuDto> getContenuByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(cahierStageContenuService.getContenuByStageId(stageId));
    }

    /**
     * Liste tous les cahiers de stage.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<CahierStageDto>> getAll() {
        return ResponseEntity.ok(cahierStageService.getAll());
    }

    /**
     * Applique la signature du stagiaire sur le cahier.
     * <p>Corps optionnel : image base64 ; sinon signature du profil utilisateur connecté.
     *
     * @param id      identifiant du cahier
     * @param request image de signature (optionnelle)
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     * @throws IllegalArgumentException si aucune signature n'est disponible (400 côté service)
     */
    @PutMapping("/{id}/signer-stagiaire")
    public ResponseEntity<CahierStageDto> signerParStagiaire(
            @PathVariable Long id,
            @RequestBody(required = false) SignerCahierRequest request) {
        return ResponseEntity.ok(cahierStageService.signerParStagiaire(id, request));
    }

    /**
     * Applique la signature de l'encadrant académique.
     *
     * @param id      identifiant du cahier
     * @param request image de signature (optionnelle)
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     */
    @PutMapping("/{id}/signer-encadrant-academique")
    public ResponseEntity<CahierStageDto> signerParEncadrantAcademique(
            @PathVariable Long id,
            @RequestBody(required = false) SignerCahierRequest request) {
        return ResponseEntity.ok(cahierStageService.signerParEncadrantAcademique(id, request));
    }

    /**
     * Applique la signature de l'encadrant professionnel.
     *
     * @param id      identifiant du cahier
     * @param request image de signature (optionnelle)
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     */
    @PutMapping("/{id}/signer-encadrant-professionnel")
    public ResponseEntity<CahierStageDto> signerParEncadrantProfessionnel(
            @PathVariable Long id,
            @RequestBody(required = false) SignerCahierRequest request) {
        return ResponseEntity.ok(cahierStageService.signerParEncadrantProfessionnel(id, request));
    }

    /**
     * Applique la signature du responsable entreprise.
     *
     * @param id      identifiant du cahier
     * @param request image de signature (optionnelle)
     * @return {@link ResponseEntity} 200 avec {@link CahierStageDto}
     */
    @PutMapping("/{id}/signer-responsable-entreprise")
    public ResponseEntity<CahierStageDto> signerParResponsableEntreprise(
            @PathVariable Long id,
            @RequestBody(required = false) SignerCahierRequest request) {
        return ResponseEntity.ok(cahierStageService.signerParResponsableEntreprise(id, request));
    }

    /**
     * Supprime un cahier de stage.
     *
     * @param id identifiant du cahier
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cahierStageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
