package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.services.CritereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des critères d'évaluation (grille de notation des fiches).
 * <p>
 * <strong>Domaine exposé :</strong> critères globaux (modèles) et critères rattachés à une fiche d'évaluation.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/criteres}
 * <p>
 * <strong>Sécurité :</strong> création/modification par {@code ENCADRANT_PROFESSIONNEL} et
 * {@code RESPONSABLE_ENTREPRISE} ; lecture élargie selon les endpoints.
 * <p>
 * <strong>Services injectés :</strong> {@link CritereEvaluationService}
 */
@RestController
@RequestMapping("/api/criteres")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CritereEvaluationController {

    private final CritereEvaluationService service;

    /**
     * Crée un critère d'évaluation (souvent rattaché à une fiche en cours de saisie).
     *
     * @param dto libellé, pondération, lien fiche éventuel
     * @return {@link ResponseEntity} 200 avec {@link CritereEvaluationDto} créé
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PostMapping
    public ResponseEntity<CritereEvaluationDto> create(@RequestBody CritereEvaluationDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Récupère un critère par identifiant.
     *
     * @param id identifiant du critère
     * @return {@link ResponseEntity} 200 avec {@link CritereEvaluationDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<CritereEvaluationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Liste tous les critères (vue gestion / entreprise).
     *
     * @return {@link ResponseEntity} 200 avec la liste des critères
     */
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @GetMapping
    public ResponseEntity<List<CritereEvaluationDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * Retourne les critères globaux (modèles sans fiche) pour initialiser une fiche d'évaluation.
     * <p>Accessible à tout utilisateur authentifié en lecture seule.
     *
     * @return {@link ResponseEntity} 200 avec les critères modèles
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/globaux")
    public ResponseEntity<List<CritereEvaluationDto>> getGlobaux() {
        return ResponseEntity.ok(service.getGlobaux());
    }

    /**
     * Liste les critères rattachés à une fiche d'évaluation donnée.
     *
     * @param ficheId identifiant de la fiche
     * @return {@link ResponseEntity} 200 avec la liste des critères de la fiche
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}")
    public ResponseEntity<List<CritereEvaluationDto>> getByFiche(@PathVariable Long ficheId) {
        return ResponseEntity.ok(service.getByFicheId(ficheId));
    }

    /**
     * Met à jour un critère existant.
     *
     * @param id  identifiant du critère
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec le critère mis à jour
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<CritereEvaluationDto> update(@PathVariable Long id,
                                                       @RequestBody CritereEvaluationDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Supprime un critère d'évaluation.
     *
     * @param id identifiant du critère
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}