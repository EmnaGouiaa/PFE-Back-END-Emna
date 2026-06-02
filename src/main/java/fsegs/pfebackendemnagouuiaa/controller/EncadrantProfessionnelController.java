package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les encadrants professionnels en entreprise.
 * <p>
 * <strong>Domaine exposé :</strong> encadrants professionnels (tuteurs en entreprise).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/encadrants-professionnels}
 * <p>
 * <strong>Sécurité :</strong> {@code ADMINISTRATEUR} pour création globale ;
 * {@code RESPONSABLE_ENTREPRISE} pour création/mise à jour/suppression dans son périmètre ;
 * lecture par identifiant ou entreprise sans restriction explicite au contrôleur.
 * <p>
 * <strong>Services injectés :</strong> {@link EncadrantProfessionnelService}
 */
@RestController
@RequestMapping("/api/encadrants-professionnels")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EncadrantProfessionnelController {

    private final EncadrantProfessionnelService encadrantProfessionnelService;

    /**
     * Crée un encadrant professionnel (administration globale).
     *
     * @param dto données validées de l'encadrant
     * @return {@link ResponseEntity} 201 avec {@link EncadrantProfessionnelDto}
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<EncadrantProfessionnelDto> create(@Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created = encadrantProfessionnelService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Crée un encadrant professionnel dans le périmètre d'un responsable entreprise.
     *
     * @param responsableId identifiant du responsable entreprise créateur
     * @param dto           données de l'encadrant
     * @return {@link ResponseEntity} 201 avec {@link EncadrantProfessionnelDto}
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PostMapping("/responsable/{responsableId}")
    public ResponseEntity<EncadrantProfessionnelDto> createByResponsable(
            @PathVariable Long responsableId,
            @Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created =
                encadrantProfessionnelService.createByResponsableEntreprise(responsableId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Met à jour un encadrant professionnel.
     *
     * @param id  identifiant de l'encadrant
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link EncadrantProfessionnelDto}
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<EncadrantProfessionnelDto> update(@PathVariable Long id,
                                                            @Valid @RequestBody EncadrantProfessionnelDto dto) {
        return ResponseEntity.ok(encadrantProfessionnelService.update(id, dto));
    }

    /**
     * Récupère un encadrant professionnel par identifiant.
     *
     * @param id identifiant de l'encadrant
     * @return {@link ResponseEntity} 200 avec {@link EncadrantProfessionnelDto}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EncadrantProfessionnelDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(encadrantProfessionnelService.getById(id));
    }

    /**
     * Liste tous les encadrants professionnels.
     *
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping
    public ResponseEntity<List<EncadrantProfessionnelDto>> getAll() {
        return ResponseEntity.ok(encadrantProfessionnelService.getAll());
    }

    /**
     * Liste les encadrants professionnels d'une entreprise.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getByEntrepriseId(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    /**
     * Alias de {@link #getByEntrepriseId(Long)} pour compatibilité front-end.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des DTO
     */
    @GetMapping("/by-entreprise/{entrepriseId}")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getByEntrepriseAlias(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    /**
     * Supprime un encadrant professionnel.
     *
     * @param id identifiant de l'encadrant
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        encadrantProfessionnelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
