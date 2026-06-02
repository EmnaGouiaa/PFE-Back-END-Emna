package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.services.ReunionFinaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des réunions finales de soutenance / clôture de stage.
 * <p>
 * <strong>Domaine exposé :</strong> réunion finale, lien avec fiches d'évaluation.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/reunions-finales}
 * <p>
 * <strong>Sécurité :</strong> création par encadrants et responsable entreprise ;
 * lecture élargie ; liste globale pour {@code RESPONSABLE_STAGE} et {@code ADMINISTRATEUR}.
 * <p>
 * <strong>Services injectés :</strong> {@link ReunionFinaleService}
 */
@RestController
@RequestMapping("/api/reunions-finales")
@RequiredArgsConstructor
public class ReunionFinaleController {

    private final ReunionFinaleService reunionFinaleService;

    /**
     * Crée une réunion finale pour un stage.
     *
     * @param dto données de la réunion (date, lieu, jury, stage)
     * @return {@link ResponseEntity} 201 avec {@link ReunionFinaleDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @PostMapping
    public ResponseEntity<ReunionFinaleDto> create(@RequestBody ReunionFinaleDto dto) {
        return new ResponseEntity<>(reunionFinaleService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère une réunion finale par identifiant.
     *
     * @param id identifiant de la réunion finale
     * @return {@link ResponseEntity} 200 avec {@link ReunionFinaleDto}
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionFinaleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionFinaleService.getById(id));
    }

    /**
     * Liste toutes les réunions finales (pilotage universitaire).
     *
     * @return {@link ResponseEntity} 200 avec la liste des réunions finales
     */
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<ReunionFinaleDto>> getAll() {
        return ResponseEntity.ok(reunionFinaleService.getAll());
    }

    /**
     * Liste les réunions finales d'un stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec la liste des réunions du stage
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionFinaleDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionFinaleService.getByStageId(stageId));
    }

    /**
     * Met à jour une réunion finale.
     *
     * @param id  identifiant de la réunion
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link ReunionFinaleDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionFinaleDto> update(@PathVariable Long id,
                                                   @RequestBody ReunionFinaleDto dto) {
        return ResponseEntity.ok(reunionFinaleService.update(id, dto));
    }

    /**
     * Supprime une réunion finale.
     *
     * @param id identifiant de la réunion
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionFinaleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
