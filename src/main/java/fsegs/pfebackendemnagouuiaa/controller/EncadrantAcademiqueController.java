package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.services.EncadrantAcademiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST CRUD pour les encadrants académiques (enseignants universitaires).
 * <p>
 * <strong>Domaine exposé :</strong> encadrants académiques rattachés aux stages.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/encadrants-academiques}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize}.
 * <p>
 * <strong>Services injectés :</strong> {@link EncadrantAcademiqueService}
 */
@RestController
@RequestMapping("/api/encadrants-academiques")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EncadrantAcademiqueController {

    private final EncadrantAcademiqueService service;

    /**
     * Crée un encadrant académique.
     *
     * @param dto données de l'encadrant
     * @return {@link ResponseEntity} 201 avec {@link EncadrantAcademiqueDto}
     */
    @PostMapping
    public ResponseEntity<EncadrantAcademiqueDto> create(@RequestBody EncadrantAcademiqueDto dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    /**
     * Met à jour un encadrant académique.
     *
     * @param id  identifiant de l'encadrant
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link EncadrantAcademiqueDto}
     */
    @PutMapping("/{id}")
    public ResponseEntity<EncadrantAcademiqueDto> update(@PathVariable Long id,
                                                         @RequestBody EncadrantAcademiqueDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Récupère un encadrant académique par identifiant.
     *
     * @param id identifiant de l'encadrant
     * @return {@link ResponseEntity} 200 avec {@link EncadrantAcademiqueDto}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EncadrantAcademiqueDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Liste tous les encadrants académiques.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<EncadrantAcademiqueDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * Supprime un encadrant académique.
     *
     * @param id identifiant de l'encadrant
     * @return {@link ResponseEntity} 204 sans corps
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
