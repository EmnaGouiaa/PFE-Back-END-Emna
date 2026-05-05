package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.StageDocumentActionResponseDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentsOverviewDto;
import fsegs.pfebackendemnagouuiaa.services.StageDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class StageDocumentController {

    private final StageDocumentService stageDocumentService;

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR')")
    public ResponseEntity<List<StageDocumentsOverviewDto>> listStageDocuments() {
        return ResponseEntity.ok(stageDocumentService.listStageDocuments());
    }

    @GetMapping("/{stageId:\\d+}/documents")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<StageDocumentsOverviewDto> getStageDocuments(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.getStageDocuments(stageId));
    }

    @PostMapping("/{stageId:\\d+}/documents/convention/generer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE')")
    public ResponseEntity<StageDocumentActionResponseDto> generateConventionPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateConventionPdf(stageId));
    }

    @GetMapping("/{stageId:\\d+}/documents/convention/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getConventionPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getConventionPdf(stageId), "convention-stage-" + stageId + ".pdf");
    }

    @PostMapping("/{stageId:\\d+}/documents/fiche-evaluation/generer")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','ENCADRANT_PROFESSIONNEL')")
    public ResponseEntity<StageDocumentActionResponseDto> generateEvaluationPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateEvaluationPdf(stageId));
    }

    @GetMapping("/{stageId:\\d+}/documents/fiche-evaluation/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getEvaluationPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getEvaluationPdf(stageId), "fiche-evaluation-" + stageId + ".pdf");
    }

    @PostMapping("/{stageId:\\d+}/documents/cahier-stage/generer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','STAGIAIRE')")
    public ResponseEntity<StageDocumentActionResponseDto> generateLogbookPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateLogbookPdf(stageId));
    }

    @GetMapping("/{stageId:\\d+}/documents/cahier-stage/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getLogbookPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getLogbookPdf(stageId), "cahier-stage-" + stageId + ".pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(content);
    }
}
