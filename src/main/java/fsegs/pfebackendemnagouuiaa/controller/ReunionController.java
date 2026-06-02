package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionEligibleParticipantDto;
import fsegs.pfebackendemnagouuiaa.services.ReunionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des réunions de suivi de stage (hors hebdomadaire / finale).
 * <p>
 * <strong>Domaine exposé :</strong> réunions, observations encadrants, comptes rendus stagiaire.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/reunions}
 * <p>
 * <strong>Sécurité :</strong> création par encadrants ; lecture pour utilisateurs authentifiés ;
 * compte rendu réservé au {@code STAGIAIRE}.
 * <p>
 * <strong>Services injectés :</strong> {@link ReunionService}
 */
@RestController
@RequestMapping("/api/reunions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReunionController {

    private final ReunionService reunionService;

    /**
     * Planifie ou enregistre une nouvelle réunion de suivi.
     *
     * @param dto données de la réunion (stage, date, participants)
     * @return {@link ResponseEntity} 201 avec {@link ReunionDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PostMapping
    public ResponseEntity<ReunionDto> create(@Valid @RequestBody ReunionDto dto) {
        return new ResponseEntity<>(reunionService.create(dto), HttpStatus.CREATED);
    }

    /**
     * Met à jour une réunion existante.
     *
     * @param id  identifiant de la réunion
     * @param dto champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link ReunionDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionDto> update(@PathVariable Long id, @Valid @RequestBody ReunionDto dto) {
        return ResponseEntity.ok(reunionService.update(id, dto));
    }

    /**
     * Récupère une réunion par identifiant.
     *
     * @param id identifiant de la réunion
     * @return {@link ResponseEntity} 200 avec {@link ReunionDto}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionService.getById(id));
    }

    /**
     * Liste toutes les réunions accessibles selon les règles du service.
     *
     * @return {@link ResponseEntity} 200 avec la liste des réunions
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ReunionDto>> getAll() {
        return ResponseEntity.ok(reunionService.getAll());
    }

    /**
     * Liste les réunions liées à un stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec la liste des réunions du stage
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionService.getByStageId(stageId));
    }

    /**
     * Liste les utilisateurs invitables à une réunion de suivi pour un stage
     * (stagiaire et encadrants ; exclut le responsable d'entreprise).
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @GetMapping("/stage/{stageId}/eligible-participants")
    public ResponseEntity<List<ReunionEligibleParticipantDto>> getEligibleParticipants(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionService.getEligibleParticipantsForStage(stageId));
    }

    /**
     * Liste les réunions du stagiaire actuellement connecté.
     *
     * @return {@link ResponseEntity} 200 avec les réunions du stagiaire authentifié
     */
    @PreAuthorize("hasRole('STAGIAIRE')")
    @GetMapping("/mes-reunions")
    public ResponseEntity<List<ReunionDto>> getPourStagiaireConnecte() {
        return ResponseEntity.ok(reunionService.getPourStagiaireAuthentifie());
    }

    /**
     * Liste les réunions visibles pour l'entreprise de l'utilisateur connecté (responsable entreprise).
     *
     * @return {@link ResponseEntity} 200 avec les réunions du périmètre entreprise
     */
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @GetMapping("/entreprise-connectee")
    public ResponseEntity<List<ReunionDto>> getPourEntrepriseConnectee() {
        return ResponseEntity.ok(reunionService.getPourEntrepriseAuthentifiee());
    }

    /**
     * Met à jour l'observation de l'encadrant créateur sur une réunion hebdomadaire
     * (académique ou professionnel, uniquement pour les réunions qu'il a planifiées).
     *
     * @param id          identifiant de la réunion
     * @param observation texte d'observation (corps brut)
     * @return {@link ResponseEntity} 200 avec {@link ReunionDto}
     */
    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}/observation")
    public ResponseEntity<ReunionDto> updateObservation(@PathVariable Long id,
                                                        @RequestBody String observation) {
        return ResponseEntity.ok(reunionService.updateObservation(id, observation));
    }

    /**
     * Met à jour le compte rendu rédigé par le stagiaire.
     *
     * @param id          identifiant de la réunion
     * @param compteRendu texte du compte rendu (corps brut)
     * @return {@link ResponseEntity} 200 avec {@link ReunionDto}
     */
    @PreAuthorize("hasRole('STAGIAIRE')")
    @PutMapping("/{id}/compte-rendu")
    public ResponseEntity<ReunionDto> updateCompteRendu(@PathVariable Long id,
                                                        @RequestBody String compteRendu) {
        return ResponseEntity.ok(reunionService.updateCompteRendu(id, compteRendu));
    }

    /**
     * Supprime une réunion.
     *
     * @param id identifiant de la réunion
     * @return {@link ResponseEntity} 204 sans corps
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
