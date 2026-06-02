package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.services.AbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des absences liées à un stage.
 * <p>
 * <strong>Domaine exposé :</strong> enregistrement et suivi des absences de stagiaires.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/absences}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize} au niveau contrôleur.
 * <p>
 * <strong>Services injectés :</strong> {@link AbsenceService}
 */
@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AbsenceController {

    private final AbsenceService absenceService;

    /**
     * Enregistre une nouvelle absence.
     *
     * @param dto données de l'absence (date, stage, justification, etc.)
     * @return {@link ResponseEntity} 201 avec {@link AbsenceDto} créée
     */
    @PostMapping
    public ResponseEntity<AbsenceDto> create(@RequestBody AbsenceDto dto) {
        return new ResponseEntity<>(absenceService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère une absence par identifiant.
     *
     * @param id identifiant de l'absence
     * @return {@link ResponseEntity} 200 avec {@link AbsenceDto}
     * @throws jakarta.persistence.EntityNotFoundException si introuvable
     */
    @GetMapping("/{id}")
    public ResponseEntity<AbsenceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceService.getById(id));
    }

    /**
     * Liste toutes les absences du système.
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link AbsenceDto}
     */
    @GetMapping
    public ResponseEntity<List<AbsenceDto>> getAll() {
        return ResponseEntity.ok(absenceService.getAll());
    }

    /**
     * Liste les absences associées à un stage donné.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec la liste des absences du stage
     */
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<AbsenceDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(absenceService.getByStageId(stageId));
    }

    /**
     * Met à jour une absence existante.
     *
     * @param id  identifiant de l'absence
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link AbsenceDto} mise à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<AbsenceDto> update(@PathVariable Long id,
                                             @RequestBody AbsenceDto dto) {
        return ResponseEntity.ok(absenceService.update(id, dto));
    }

    /**
     * Supprime une absence.
     *
     * @param id identifiant de l'absence
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        absenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
