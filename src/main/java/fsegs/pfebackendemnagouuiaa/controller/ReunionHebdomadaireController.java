package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.services.ReunionHebdomadaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des réunions hebdomadaires de suivi (cahier de stage).
 * <p>
 * <strong>Domaine exposé :</strong> séances hebdomadaires liées à un stage ou à un cahier de stage.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/reunions-hebdomadaires}
 * <p>
 * <strong>Sécurité :</strong> rôles multiples selon lecture/écriture (stagiaire, encadrants, responsables).
 * <p>
 * <strong>Services injectés :</strong> {@link ReunionHebdomadaireService}
 */
@RestController
@RequestMapping("/api/reunions-hebdomadaires")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReunionHebdomadaireController {

    private final ReunionHebdomadaireService reunionHebdomadaireService;

    /**
     * Crée une réunion hebdomadaire.
     *
     * @param dto données de la séance
     * @return {@link ResponseEntity} 201 avec {@link ReunionHebdomadaireDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<ReunionHebdomadaireDto> create(@RequestBody ReunionHebdomadaireDto dto) {
        return new ResponseEntity<>(reunionHebdomadaireService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère une réunion hebdomadaire par identifiant.
     *
     * @param id identifiant de la réunion
     * @return {@link ResponseEntity} 200 avec {@link ReunionHebdomadaireDto}
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionHebdomadaireDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionHebdomadaireService.getById(id));
    }

    /**
     * Liste toutes les réunions hebdomadaires (vue gestion).
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<ReunionHebdomadaireDto>> getAll() {
        return ResponseEntity.ok(reunionHebdomadaireService.getAll());
    }

    /**
     * Liste les réunions hebdomadaires d'un stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec la liste des séances
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionHebdomadaireDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionHebdomadaireService.getByStageId(stageId));
    }

    /**
     * Liste les réunions hebdomadaires rattachées à un cahier de stage.
     *
     * @param cahierStageId identifiant du cahier de stage
     * @return {@link ResponseEntity} 200 avec la liste des séances
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/cahier-stage/{cahierStageId}")
    public ResponseEntity<List<ReunionHebdomadaireDto>> getByCahierStageId(@PathVariable Long cahierStageId) {
        return ResponseEntity.ok(reunionHebdomadaireService.getByCahierStageId(cahierStageId));
    }

    /**
     * Met à jour une réunion hebdomadaire.
     *
     * @param id  identifiant de la réunion
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link ReunionHebdomadaireDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionHebdomadaireDto> update(@PathVariable Long id,
                                                         @RequestBody ReunionHebdomadaireDto dto) {
        return ResponseEntity.ok(reunionHebdomadaireService.update(id, dto));
    }

    /**
     * Supprime une réunion hebdomadaire.
     *
     * @param id identifiant de la réunion
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionHebdomadaireService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
