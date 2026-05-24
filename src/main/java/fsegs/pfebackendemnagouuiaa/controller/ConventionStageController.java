package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.services.ConventionStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conventions-stage")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ConventionStageController {

    private final ConventionStageService conventionStageService;

    @PostMapping
    public ResponseEntity<ConventionStageDto> create(@RequestBody ConventionStageDto dto) {
        return new ResponseEntity<>(conventionStageService.create(dto), HttpStatus.CREATED);
    }

    @PostMapping("/stage/{stageId}")
    public ResponseEntity<ConventionStageDto> createByStage(@PathVariable Long stageId,
                                                            @RequestBody ConventionStageDto dto) {
        return new ResponseEntity<>(conventionStageService.createByStage(stageId, dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConventionStageDto> update(@PathVariable Long id,
                                                     @RequestBody ConventionStageDto dto) {
        return ResponseEntity.ok(conventionStageService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConventionStageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.getById(id));
    }

    @GetMapping("/stage/{stageId}")
    public ResponseEntity<ConventionStageDto> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(conventionStageService.getByStageId(stageId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<ConventionStageDto>> getAll() {
        return ResponseEntity.ok(conventionStageService.getAll());
    }

    @PutMapping("/{id}/signer-stagiaire")
    public ResponseEntity<ConventionStageDto> signerParStagiaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParStagiaire(id));
    }

    @PutMapping("/{id}/signer-encadrant-academique")
    public ResponseEntity<ConventionStageDto> signerParEncadrantAcademique(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEncadrantAcademique(id));
    }

    @PutMapping("/{id}/signer-encadrant-professionnel")
    public ResponseEntity<ConventionStageDto> signerParEncadrantProfessionnel(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEncadrantProfessionnel(id));
    }

    @PutMapping("/{id}/signer-entreprise")
    public ResponseEntity<ConventionStageDto> signerParEntreprise(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParEntreprise(id));
    }

    @PutMapping("/{id}/signer-responsable")
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    public ResponseEntity<ConventionStageDto> signerParResponsable(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParResponsable(id));
    }

    @PatchMapping("/{id}/signer-responsable-universitaire")
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    public ResponseEntity<ConventionStageDto> signerParResponsableUniversitaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionStageService.signerParResponsable(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conventionStageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
