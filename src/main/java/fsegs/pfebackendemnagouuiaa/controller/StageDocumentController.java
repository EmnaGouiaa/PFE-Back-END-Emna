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

/**
 * Contrôleur REST de génération et téléchargement des documents PDF liés à un stage.
 * <p>
 * <strong>Domaine exposé :</strong> convention, fiche d'évaluation, cahier de stage (PDF).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/stages} (même préfixe que {@link StageController})
 * <p>
 * <strong>Sécurité :</strong> rôles acteurs du stage pour un stage donné ;
 * vue globale réservée à {@code RESPONSABLE_STAGE} et {@code ADMINISTRATEUR}.
 * <p>
 * <strong>Services injectés :</strong> {@link StageDocumentService}
 */
@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class StageDocumentController {

    private final StageDocumentService stageDocumentService;

    /**
     * Liste la vue d'ensemble des documents pour tous les stages (pilotage universitaire).
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link StageDocumentsOverviewDto}
     */
    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR')")
    public ResponseEntity<List<StageDocumentsOverviewDto>> listStageDocuments() {
        return ResponseEntity.ok(stageDocumentService.listStageDocuments());
    }

    /**
     * Retourne l'état des documents PDF pour un stage donné.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link StageDocumentsOverviewDto}
     */
    @GetMapping("/{stageId:\\d+}/documents")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<StageDocumentsOverviewDto> getStageDocuments(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.getStageDocuments(stageId));
    }

    /**
     * Génère ou régénère le PDF de convention de stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link StageDocumentActionResponseDto}
     */
    @PostMapping("/{stageId:\\d+}/documents/convention/generer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<StageDocumentActionResponseDto> generateConventionPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateConventionPdf(stageId));
    }

    /**
     * Télécharge le PDF de convention (affichage inline dans le navigateur).
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec le flux binaire PDF
     */
    @GetMapping("/{stageId:\\d+}/documents/convention/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getConventionPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getConventionPdf(stageId), "convention-stage-" + stageId + ".pdf");
    }

    /**
     * Génère ou régénère le PDF de fiche d'évaluation.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link StageDocumentActionResponseDto}
     */
    @PostMapping("/{stageId:\\d+}/documents/fiche-evaluation/generer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<StageDocumentActionResponseDto> generateEvaluationPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateEvaluationPdf(stageId));
    }

    /**
     * Télécharge le PDF de fiche d'évaluation.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec le flux binaire PDF
     */
    @GetMapping("/{stageId:\\d+}/documents/fiche-evaluation/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getEvaluationPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getEvaluationPdf(stageId), "fiche-evaluation-" + stageId + ".pdf");
    }

    /**
     * Génère ou régénère le PDF du cahier de stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec {@link StageDocumentActionResponseDto}
     */
    @PostMapping("/{stageId:\\d+}/documents/cahier-stage/generer")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<StageDocumentActionResponseDto> generateLogbookPdf(@PathVariable Long stageId) {
        return ResponseEntity.ok(stageDocumentService.generateLogbookPdf(stageId));
    }

    /**
     * Télécharge le PDF du cahier de stage.
     *
     * @param stageId identifiant du stage
     * @return {@link ResponseEntity} 200 avec le flux binaire PDF
     */
    @GetMapping("/{stageId:\\d+}/documents/cahier-stage/pdf")
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE','ADMINISTRATEUR','ENCADRANT_ACADEMIQUE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','STAGIAIRE')")
    public ResponseEntity<byte[]> getLogbookPdf(@PathVariable Long stageId) {
        return buildPdfResponse(stageDocumentService.getLogbookPdf(stageId), "cahier-stage-" + stageId + ".pdf");
    }

    /**
     * Construit une réponse HTTP PDF avec en-têtes d'affichage inline.
     *
     * @param content  octets du PDF
     * @param filename nom suggéré pour le navigateur
     * @return {@link ResponseEntity} 200 {@code application/pdf}
     */
    private ResponseEntity<byte[]> buildPdfResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(content);
    }
}
