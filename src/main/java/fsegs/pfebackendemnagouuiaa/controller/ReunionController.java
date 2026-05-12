package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.services.ReunionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reunions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReunionController {

    private final ReunionService reunionService;

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PostMapping
    public ResponseEntity<ReunionDto> create(@Valid @RequestBody ReunionDto dto) {
        return new ResponseEntity<>(reunionService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionDto> update(@PathVariable Long id, @Valid @RequestBody ReunionDto dto) {
        return ResponseEntity.ok(reunionService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionService.getById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ReunionDto>> getAll() {
        return ResponseEntity.ok(reunionService.getAll());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionService.getByStageId(stageId));
    }

    @PreAuthorize("hasRole('STAGIAIRE')")
    @GetMapping("/mes-reunions")
    public ResponseEntity<List<ReunionDto>> getPourStagiaireConnecte() {
        return ResponseEntity.ok(reunionService.getPourStagiaireAuthentifie());
    }

    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @GetMapping("/entreprise-connectee")
    public ResponseEntity<List<ReunionDto>> getPourEntrepriseConnectee() {
        return ResponseEntity.ok(reunionService.getPourEntrepriseAuthentifiee());
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}/observation")
    public ResponseEntity<ReunionDto> updateObservation(@PathVariable Long id,
                                                        @RequestBody String observation) {
        return ResponseEntity.ok(reunionService.updateObservation(id, observation));
    }

    @PreAuthorize("hasRole('STAGIAIRE')")
    @PutMapping("/{id}/compte-rendu")
    public ResponseEntity<ReunionDto> updateCompteRendu(@PathVariable Long id,
                                                        @RequestBody String compteRendu) {
        return ResponseEntity.ok(reunionService.updateCompteRendu(id, compteRendu));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
