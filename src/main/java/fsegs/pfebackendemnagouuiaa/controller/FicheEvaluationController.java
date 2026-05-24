package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.services.FicheEvaluationService;
import fsegs.pfebackendemnagouuiaa.services.FicheEvaluationPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiches-evaluation")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FicheEvaluationController {

    private final FicheEvaluationService    ficheEvaluationService;
    private final FicheEvaluationPdfService ficheEvaluationPdfService;

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE')")
    @PostMapping
    public ResponseEntity<FicheEvaluationDto> create(@RequestBody FicheEvaluationDto dto) {
        return new ResponseEntity<>(ficheEvaluationService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<FicheEvaluationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ficheEvaluationService.getById(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_STAGE')")
    @GetMapping
    public ResponseEntity<List<FicheEvaluationDto>> getAll() {
        return ResponseEntity.ok(ficheEvaluationService.getAll());
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<FicheEvaluationDto> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(ficheEvaluationService.getByStageId(stageId));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
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

    @PreAuthorize("hasRole('ENCADRANT_PROFESSIONNEL')")
    @PutMapping("/{id}/notes/{userId}")
    public ResponseEntity<FicheEvaluationDto> enregistrerNotes(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody List<NoteAttribueeDto> notes) {
        return ResponseEntity.ok(ficheEvaluationService.enregistrerNotesEncadrantProfessionnel(id, userId, notes));
    }

    @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}/notes-re/{userId}")
    public ResponseEntity<FicheEvaluationDto> enregistrerNotesRE(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody List<NoteAttribueeDto> notes) {
        return ResponseEntity.ok(ficheEvaluationService.enregistrerNotesResponsableEntreprise(id, userId, notes));
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> telechargerPdfParId(@PathVariable Long id) {
        try {
            FicheEvaluationDto fiche = ficheEvaluationService.getById(id);
            byte[] pdf = ficheEvaluationPdfService.generer(fiche);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"fiche-evaluation-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE','STAGIAIRE','RESPONSABLE_STAGE','ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}/pdf")
    public ResponseEntity<byte[]> telechargerPdfParStage(@PathVariable Long stageId) {
        try {
            FicheEvaluationDto fiche = ficheEvaluationService.getByStageId(stageId);
            byte[] pdf = ficheEvaluationPdfService.generer(fiche);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"fiche-evaluation-stage-" + stageId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
