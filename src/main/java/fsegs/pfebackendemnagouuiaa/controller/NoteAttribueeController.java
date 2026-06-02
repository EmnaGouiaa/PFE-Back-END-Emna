package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.services.NoteAttribueeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des notes attribuées par critère sur une fiche d'évaluation.
 * <p>
 * <strong>Domaine exposé :</strong> couple (fiche, critère) → note et commentaire.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/notes-attribuees}
 * <p>
 * <strong>Sécurité :</strong> saisie par {@code ENCADRANT_PROFESSIONNEL} ; lecture élargie aux acteurs du stage.
 * <p>
 * <strong>Services injectés :</strong> {@link NoteAttribueeService}
 */
@RestController
@RequestMapping("/api/notes-attribuees")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NoteAttribueeController {

    private final NoteAttribueeService noteAttribueeService;

    /**
     * Enregistre une note sur un critère pour une fiche (encadrant professionnel).
     *
     * @param dto fiche, critère, valeur de note
     * @return {@link ResponseEntity} 201 avec {@link NoteAttribueeDto}
     */
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PostMapping
    public ResponseEntity<NoteAttribueeDto> create(@RequestBody NoteAttribueeDto dto) {
        return new ResponseEntity<>(noteAttribueeService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Liste toutes les notes attribuées (vue transverse).
     *
     * @return {@link ResponseEntity} 200 avec la liste des notes
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<NoteAttribueeDto>> getAll() {
        return ResponseEntity.ok(noteAttribueeService.getAll());
    }

    /**
     * Récupère la note pour un couple fiche / critère.
     *
     * @param ficheId   identifiant de la fiche d'évaluation
     * @param critereId identifiant du critère
     * @return {@link ResponseEntity} 200 avec {@link NoteAttribueeDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<NoteAttribueeDto> getById(@PathVariable Long ficheId,
                                                    @PathVariable Long critereId) {
        return ResponseEntity.ok(noteAttribueeService.getById(ficheId, critereId));
    }

    /**
     * Liste toutes les notes d'une fiche d'évaluation.
     *
     * @param ficheId identifiant de la fiche
     * @return {@link ResponseEntity} 200 avec la liste des notes de la fiche
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}")
    public ResponseEntity<List<NoteAttribueeDto>> getByFicheId(@PathVariable Long ficheId) {
        return ResponseEntity.ok(noteAttribueeService.getByFicheEvaluationId(ficheId));
    }

    /**
     * Liste les notes liées à un critère donné (toutes fiches confondues).
     *
     * @param critereId identifiant du critère
     * @return {@link ResponseEntity} 200 avec la liste des notes
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/critere/{critereId}")
    public ResponseEntity<List<NoteAttribueeDto>> getByCritereId(@PathVariable Long critereId) {
        return ResponseEntity.ok(noteAttribueeService.getByCritereEvaluationId(critereId));
    }

    /**
     * Met à jour une note existante (encadrant professionnel).
     *
     * @param ficheId   identifiant de la fiche
     * @param critereId identifiant du critère
     * @param dto       nouvelle valeur / commentaire
     * @return {@link ResponseEntity} 200 avec {@link NoteAttribueeDto}
     */
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<NoteAttribueeDto> update(@PathVariable Long ficheId,
                                                   @PathVariable Long critereId,
                                                   @RequestBody NoteAttribueeDto dto) {
        return ResponseEntity.ok(noteAttribueeService.update(ficheId, critereId, dto));
    }

    /**
     * Supprime une note attribuée.
     *
     * @param ficheId   identifiant de la fiche
     * @param critereId identifiant du critère
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','ADMINISTRATEUR')")
    @DeleteMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<Void> delete(@PathVariable Long ficheId,
                                       @PathVariable Long critereId) {
        noteAttribueeService.delete(ficheId, critereId);
        return ResponseEntity.noContent().build();
    }
}
