package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleFormulairesUpdateRequest;
import fsegs.pfebackendemnagouuiaa.services.ReunionFinaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reunions-finales")
@RequiredArgsConstructor
public class ReunionFinaleController {

    private final ReunionFinaleService reunionFinaleService;

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @PostMapping
    public ResponseEntity<ReunionFinaleDto> create(@RequestBody ReunionFinaleDto dto) {
        return new ResponseEntity<>(reunionFinaleService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionFinaleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionFinaleService.getById(id));
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<ReunionFinaleDto>> getAll() {
        return ResponseEntity.ok(reunionFinaleService.getAll());
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionFinaleDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionFinaleService.getByStageId(stageId));
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES','ADMINISTRATEUR')")
    @PatchMapping("/{id}/formulaires")
    public ResponseEntity<ReunionFinaleDto> updateFormulaires(@PathVariable Long id,
                                                              @RequestBody ReunionFinaleFormulairesUpdateRequest request) {
        return ResponseEntity.ok(reunionFinaleService.updateFormulaires(id, request));
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @GetMapping("/{id}/enquete-satisfaction")
    public ResponseEntity<EnqueteSatisfactionDto> getEnqueteSatisfaction(@PathVariable Long id) {
        return ResponseEntity.ok(reunionFinaleService.getEnqueteSatisfaction(id));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionFinaleDto> update(@PathVariable Long id,
                                                   @RequestBody ReunionFinaleDto dto) {
        return ResponseEntity.ok(reunionFinaleService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionFinaleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
