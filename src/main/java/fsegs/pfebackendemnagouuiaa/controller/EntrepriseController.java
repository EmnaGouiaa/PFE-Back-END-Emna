package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.services.EntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST CRUD pour les entreprises partenaires.
 * <p>
 * <strong>Domaine exposé :</strong> fiches entreprise (raison sociale, coordonnées, etc.).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/entreprises}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize} ; contrôle via configuration globale.
 * <p>
 * <strong>Services injectés :</strong> {@link EntrepriseService}
 */
@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    /**
     * Crée une nouvelle entreprise.
     *
     * @param dto données validées de l'entreprise
     * @return {@link ResponseEntity} 201 avec {@link EntrepriseDto} créée
     */
    @PostMapping
    public ResponseEntity<EntrepriseDto> create(@Valid @RequestBody EntrepriseDto dto) {
        EntrepriseDto created = entrepriseService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Met à jour une entreprise existante.
     *
     * @param id  identifiant de l'entreprise
     * @param dto champs à modifier
     * @return {@link ResponseEntity} 200 avec {@link EntrepriseDto} mise à jour
     * @throws jakarta.persistence.EntityNotFoundException si l'entreprise est introuvable
     */
    @PutMapping("/{id}")
    public ResponseEntity<EntrepriseDto> update(@PathVariable Long id, @Valid @RequestBody EntrepriseDto dto) {
        EntrepriseDto updated = entrepriseService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère une entreprise par identifiant.
     *
     * @param id identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec {@link EntrepriseDto}
     * @throws jakarta.persistence.EntityNotFoundException si introuvable
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntrepriseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseService.getById(id));
    }

    /**
     * Liste toutes les entreprises.
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link EntrepriseDto}
     */
    @GetMapping
    public ResponseEntity<List<EntrepriseDto>> getAll() {
        return ResponseEntity.ok(entrepriseService.getAll());
    }

    /**
     * Supprime une entreprise.
     *
     * @param id identifiant de l'entreprise
     * @return {@link ResponseEntity} 204 sans corps
     * @throws jakarta.persistence.EntityNotFoundException si introuvable
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
