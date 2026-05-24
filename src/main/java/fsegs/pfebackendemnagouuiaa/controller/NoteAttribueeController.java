package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.services.NoteAttribueeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes-attribuees")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NoteAttribueeController {

    private final NoteAttribueeService noteAttribueeService;

    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PostMapping
    public ResponseEntity<NoteAttribueeDto> create(@RequestBody NoteAttribueeDto dto) {
        return new ResponseEntity<>(noteAttribueeService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<NoteAttribueeDto>> getAll() {
        return ResponseEntity.ok(noteAttribueeService.getAll());
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<NoteAttribueeDto> getById(@PathVariable Long ficheId,
                                                    @PathVariable Long critereId) {
        return ResponseEntity.ok(noteAttribueeService.getById(ficheId, critereId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/fiche/{ficheId}")
    public ResponseEntity<List<NoteAttribueeDto>> getByFicheId(@PathVariable Long ficheId) {
        return ResponseEntity.ok(noteAttribueeService.getByFicheEvaluationId(ficheId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/critere/{critereId}")
    public ResponseEntity<List<NoteAttribueeDto>> getByCritereId(@PathVariable Long critereId) {
        return ResponseEntity.ok(noteAttribueeService.getByCritereEvaluationId(critereId));
    }

    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<NoteAttribueeDto> update(@PathVariable Long ficheId,
                                                   @PathVariable Long critereId,
                                                   @RequestBody NoteAttribueeDto dto) {
        return ResponseEntity.ok(noteAttribueeService.update(ficheId, critereId, dto));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','ADMINISTRATEUR')")
    @DeleteMapping("/fiche/{ficheId}/critere/{critereId}")
    public ResponseEntity<Void> delete(@PathVariable Long ficheId,
                                       @PathVariable Long critereId) {
        noteAttribueeService.delete(ficheId, critereId);
        return ResponseEntity.noContent().build();
    }
}
