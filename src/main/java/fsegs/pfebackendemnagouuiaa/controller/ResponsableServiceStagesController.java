package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;
import fsegs.pfebackendemnagouuiaa.services.ResponsableServiceStagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST CRUD pour les responsables du service des stages universitaires.
 * <p>
 * <strong>Domaine exposé :</strong> responsables service stages (validation offres, conventions, etc.).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/responsables-service-stages}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize}.
 * <p>
 * <strong>Services injectés :</strong> {@link ResponsableServiceStagesService}
 */
@RestController
@RequestMapping("/api/responsables-service-stages")
@RequiredArgsConstructor
public class ResponsableServiceStagesController {

    private final ResponsableServiceStagesService service;

    /**
     * Crée un responsable du service des stages.
     *
     * @param dto données de création
     * @return {@link ResponseEntity} 201 avec {@link ResponsableServiceStagesResponseDTO}
     */
    @PostMapping
    public ResponseEntity<ResponsableServiceStagesResponseDTO> create(
            @RequestBody ResponsableServiceStagesRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    /**
     * Récupère un responsable par identifiant.
     *
     * @param id identifiant du responsable
     * @return {@link ResponseEntity} 200 avec le DTO de réponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponsableServiceStagesResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Liste tous les responsables du service des stages.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<ResponsableServiceStagesResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * Met à jour un responsable existant.
     *
     * @param id  identifiant du responsable
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec le DTO mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponsableServiceStagesResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ResponsableServiceStagesRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Supprime un responsable du service des stages.
     *
     * @param id identifiant du responsable
     * @return {@link ResponseEntity} 200 avec message de confirmation (texte brut)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok("ResponsableServiceStages supprimé avec succés");
    }
}
