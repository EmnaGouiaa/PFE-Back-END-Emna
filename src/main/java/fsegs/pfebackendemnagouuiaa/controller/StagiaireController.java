package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AffectationEncadrantAcademiqueResponse;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;
import fsegs.pfebackendemnagouuiaa.services.StagiaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stagiaires")
@RequiredArgsConstructor

public class StagiaireController {
    private final StagiaireService stagiaireService;

    @PostMapping
    public ResponseEntity<StagiaireResponseDTO> create(@RequestBody StagiaireRequestDTO dto) {
        return new ResponseEntity<>(stagiaireService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StagiaireResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stagiaireService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<StagiaireResponseDTO>> getAll() {
        return ResponseEntity.ok(stagiaireService.getAll());
    }

    @GetMapping("/sans-stage")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<StagiaireResponseDTO>> getSansStage() {
        return ResponseEntity.ok(stagiaireService.getSansStage());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<StagiaireResponseDTO> searchByEmail(@RequestParam String email) {
        return ResponseEntity.ok(stagiaireService.searchByEmail(email));
    }

    @PatchMapping("/{id}/affecter-encadrant-academique/{encadrantId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<AffectationEncadrantAcademiqueResponse> affecterEncadrantAcademique(
            @PathVariable Long id,
            @PathVariable Long encadrantId
    ) {
        return ResponseEntity.ok(stagiaireService.affecterEncadrantAcademique(id, encadrantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StagiaireResponseDTO> update(@PathVariable Long id,
                                                       @RequestBody StagiaireRequestDTO dto) {
        return ResponseEntity.ok(stagiaireService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        stagiaireService.delete(id);
        return ResponseEntity.ok("Stagiaire supprimé avec succés");
    }
}
