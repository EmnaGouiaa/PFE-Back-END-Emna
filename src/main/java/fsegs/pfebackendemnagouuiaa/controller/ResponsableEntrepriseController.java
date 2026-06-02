package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.services.ResponsableEntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les responsables d'entreprise (représentants légaux / RH).
 * <p>
 * <strong>Domaine exposé :</strong> responsables entreprise rattachés aux fiches entreprise.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/responsables-entreprise}
 * <p>
 * <strong>Sécurité :</strong> écriture ({@code POST}, {@code PUT}, {@code DELETE}) réservée à
 * {@code ADMINISTRATEUR} ; lecture ouverte au niveau contrôleur.
 * <p>
 * <strong>Services injectés :</strong> {@link ResponsableEntrepriseService}
 */
@RestController
@RequestMapping("/api/responsables-entreprise")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ResponsableEntrepriseController {

    private final ResponsableEntrepriseService responsableEntrepriseService;

    /**
     * Crée un responsable entreprise.
     *
     * @param dto données validées
     * @return {@link ResponseEntity} 201 avec {@link ResponsableEntrepriseDto}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<ResponsableEntrepriseDto> create(@Valid @RequestBody ResponsableEntrepriseDto dto) {
        ResponsableEntrepriseDto created = responsableEntrepriseService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Met à jour un responsable entreprise.
     *
     * @param id  identifiant du responsable
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link ResponsableEntrepriseDto}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponsableEntrepriseDto> update(@PathVariable Long id,
                                                           @Valid @RequestBody ResponsableEntrepriseDto dto) {
        return ResponseEntity.ok(responsableEntrepriseService.update(id, dto));
    }

    /**
     * Récupère un responsable entreprise par identifiant.
     *
     * @param id identifiant du responsable
     * @return {@link ResponseEntity} 200 avec {@link ResponsableEntrepriseDto}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponsableEntrepriseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(responsableEntrepriseService.getById(id));
    }

    /**
     * Liste tous les responsables entreprise.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<ResponsableEntrepriseDto>> getAll() {
        return ResponseEntity.ok(responsableEntrepriseService.getAll());
    }

    /**
     * Liste les responsables d'une entreprise donnée.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<ResponsableEntrepriseDto>> getByEntrepriseId(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(responsableEntrepriseService.getByEntrepriseId(entrepriseId));
    }

    /**
     * Supprime un responsable entreprise.
     *
     * @param id identifiant du responsable
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        responsableEntrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
