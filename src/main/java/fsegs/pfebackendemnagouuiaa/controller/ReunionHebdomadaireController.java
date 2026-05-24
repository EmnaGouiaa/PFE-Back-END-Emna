package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.services.ReunionHebdomadaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reunions-hebdomadaires")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReunionHebdomadaireController {

    private final ReunionHebdomadaireService reunionHebdomadaireService;

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<ReunionHebdomadaireDto> create(@RequestBody ReunionHebdomadaireDto dto) {
        return new ResponseEntity<>(reunionHebdomadaireService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionHebdomadaireDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reunionHebdomadaireService.getById(id));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<ReunionHebdomadaireDto>> getAll() {
        return ResponseEntity.ok(reunionHebdomadaireService.getAll());
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<ReunionHebdomadaireDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(reunionHebdomadaireService.getByStageId(stageId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @GetMapping("/cahier-stage/{cahierStageId}")
    public ResponseEntity<List<ReunionHebdomadaireDto>> getByCahierStageId(@PathVariable Long cahierStageId) {
        return ResponseEntity.ok(reunionHebdomadaireService.getByCahierStageId(cahierStageId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_STAGE','RESPONSABLE_ENTREPRISE','ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionHebdomadaireDto> update(@PathVariable Long id,
                                                         @RequestBody ReunionHebdomadaireDto dto) {
        return ResponseEntity.ok(reunionHebdomadaireService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reunionHebdomadaireService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
