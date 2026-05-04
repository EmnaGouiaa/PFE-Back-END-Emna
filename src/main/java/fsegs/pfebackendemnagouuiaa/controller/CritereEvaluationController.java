package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.services.CritereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/criteres")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CritereEvaluationController {

    private final CritereEvaluationService service;

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PostMapping
    public ResponseEntity<CritereEvaluationDto> create(@RequestBody CritereEvaluationDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<CritereEvaluationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @GetMapping
    public ResponseEntity<List<CritereEvaluationDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}")
    public ResponseEntity<List<CritereEvaluationDto>> getByFiche(@PathVariable Long ficheId) {
        return ResponseEntity.ok(service.getByFicheId(ficheId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<CritereEvaluationDto> update(@PathVariable Long id,
                                                       @RequestBody CritereEvaluationDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}