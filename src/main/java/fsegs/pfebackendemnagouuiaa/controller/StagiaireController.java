package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AffectationEncadrantAcademiqueResponse;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;
import fsegs.pfebackendemnagouuiaa.services.StagiaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des stagiaires (étudiants en stage).
 * <p>
 * <strong>Domaine exposé :</strong> fiches stagiaires, affectation d'encadrant académique, recherche.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/stagiaires}
 * <p>
 * <strong>Sécurité :</strong> rôles {@code RESPONSABLE_STAGE} et {@code ADMINISTRATEUR} pour les
 * opérations de pilotage ; CRUD de base sans {@code @PreAuthorize} explicite.
 * <p>
 * <strong>Services injectés :</strong> {@link StagiaireService}
 */
@RestController
@RequestMapping("/api/stagiaires")
@RequiredArgsConstructor
public class StagiaireController {

    private final StagiaireService stagiaireService;

    /**
     * Crée un nouveau stagiaire.
     *
     * @param dto données du stagiaire
     * @return {@link ResponseEntity} 201 avec {@link StagiaireResponseDTO}
     */
    @PostMapping
    public ResponseEntity<StagiaireResponseDTO> create(@Valid @RequestBody StagiaireRequestDTO dto) {
        return new ResponseEntity<>(stagiaireService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère un stagiaire par identifiant.
     *
     * @param id identifiant du stagiaire
     * @return {@link ResponseEntity} 200 avec {@link StagiaireResponseDTO}
     */
    @GetMapping("/{id}")
    public ResponseEntity<StagiaireResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stagiaireService.getById(id));
    }

    /**
     * Liste tous les stagiaires.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<StagiaireResponseDTO>> getAll() {
        return ResponseEntity.ok(stagiaireService.getAll());
    }

    /**
     * Liste les stagiaires n'ayant pas encore de stage actif.
     * <p>Réservé au pilotage universitaire ({@code RESPONSABLE_STAGE}, {@code ADMINISTRATEUR}).
     *
     * @return {@link ResponseEntity} 200 avec la liste des stagiaires sans stage
     */
    @GetMapping("/sans-stage")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<StagiaireResponseDTO>> getSansStage() {
        return ResponseEntity.ok(stagiaireService.getSansStage());
    }

    /**
     * Recherche un stagiaire par adresse e-mail.
     *
     * @param email adresse e-mail exacte ou critère attendu par le service
     * @return {@link ResponseEntity} 200 avec {@link StagiaireResponseDTO}
     * @throws jakarta.persistence.EntityNotFoundException si aucun stagiaire ne correspond
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<StagiaireResponseDTO> searchByEmail(@RequestParam String email) {
        return ResponseEntity.ok(stagiaireService.searchByEmail(email));
    }

    /**
     * Affecte un encadrant académique à un stagiaire.
     *
     * @param id          identifiant du stagiaire
     * @param encadrantId identifiant de l'encadrant académique
     * @return {@link ResponseEntity} 200 avec {@link AffectationEncadrantAcademiqueResponse}
     */
    @PatchMapping("/{id}/affecter-encadrant-academique/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<AffectationEncadrantAcademiqueResponse> affecterEncadrantAcademique(
            @PathVariable Long id,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(stagiaireService.affecterEncadrantAcademique(id, encadrantId));
    }

    /**
     * Met à jour les informations d'un stagiaire.
     *
     * @param id  identifiant du stagiaire
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link StagiaireResponseDTO}
     */
    @PutMapping("/{id}")
    public ResponseEntity<StagiaireResponseDTO> update(@PathVariable Long id,
                                                       @Valid @RequestBody StagiaireRequestDTO dto) {
        return ResponseEntity.ok(stagiaireService.update(id, dto));
    }

    /**
     * Supprime un stagiaire.
     *
     * @param id identifiant du stagiaire
     * @return {@link ResponseEntity} 200 avec message texte de confirmation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        stagiaireService.delete(id);
        return ResponseEntity.ok("Stagiaire supprimé avec succés");
    }
}
