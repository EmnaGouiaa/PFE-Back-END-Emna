package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.StageDocumentActionResponseDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentsOverviewDto;

import java.util.List;

public interface StageDocumentService {

    List<StageDocumentsOverviewDto> listStageDocuments();

    StageDocumentsOverviewDto getStageDocuments(Long stageId);

    StageDocumentActionResponseDto generateConventionPdf(Long stageId);

    StageDocumentActionResponseDto generateEvaluationPdf(Long stageId);

    StageDocumentActionResponseDto generateLogbookPdf(Long stageId);

    byte[] getConventionPdf(Long stageId);

    byte[] getEvaluationPdf(Long stageId);

    byte[] getLogbookPdf(Long stageId);
}
