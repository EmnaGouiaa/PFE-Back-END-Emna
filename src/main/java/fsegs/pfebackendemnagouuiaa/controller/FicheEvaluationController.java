package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.services.FicheEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiches-evaluation")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FicheEvaluationController {

    private final FicheEvaluationService ficheEvaluationService;

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PostMapping
    public ResponseEntity<FicheEvaluationDto> create(@RequestBody FicheEvaluationDto dto) {
        return new ResponseEntity<>(ficheEvaluationService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<FicheEvaluationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ficheEvaluationService.getById(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES')")
    @GetMapping
    public ResponseEntity<List<FicheEvaluationDto>> getAll() {
        return ResponseEntity.ok(ficheEvaluationService.getAll());
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<FicheEvaluationDto> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(ficheEvaluationService.getByStageId(stageId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/reunion-finale/{reunionFinaleId}")
    public ResponseEntity<List<FicheEvaluationDto>> getByReunionFinaleId(@PathVariable Long reunionFinaleId) {
        return ResponseEntity.ok(ficheEvaluationService.getByReunionFinaleId(reunionFinaleId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<FicheEvaluationDto> update(@PathVariable Long id,
                                                     @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.update(id, dto));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/signer/{userId}")
    public ResponseEntity<FicheEvaluationDto> signerFiche(@PathVariable Long id,
                                                          @PathVariable Long userId) {
        return ResponseEntity.ok(ficheEvaluationService.signerFiche(id, userId));
    }
    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PatchMapping("/{id}/encadrant-pro/{userId}")
    public ResponseEntity<FicheEvaluationDto> remplirPartieEncadrantPro(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.remplirPartieEncadrantProfessionnel(id, userId, dto));
    }
    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/responsable-entreprise/{userId}")
    public ResponseEntity<FicheEvaluationDto> remplirPartieResponsableEntreprise(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody FicheEvaluationDto dto) {
        return ResponseEntity.ok(ficheEvaluationService.remplirPartieResponsableEntreprise(id, userId, dto));
    }
}
