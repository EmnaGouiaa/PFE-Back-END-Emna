package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;

import java.util.List;

public interface FicheEvaluationService {

    FicheEvaluationDto create(FicheEvaluationDto dto);

    FicheEvaluationDto getById(Long id);

    List<FicheEvaluationDto> getAll();

    FicheEvaluationDto getByStageId(Long stageId);

    List<FicheEvaluationDto> getByReunionFinaleId(Long reunionFinaleId);

    FicheEvaluationDto update(Long id, FicheEvaluationDto dto);

    FicheEvaluationDto signerFiche(Long ficheId, Long userId);
    FicheEvaluationDto remplirPartieEncadrantProfessionnel(Long ficheId, Long userId, FicheEvaluationDto dto);
    FicheEvaluationDto remplirPartieResponsableEntreprise(Long ficheId, Long userId, FicheEvaluationDto dto);
    FicheEvaluationDto enregistrerNotesEncadrantProfessionnel(Long ficheId, Long userId, List<NoteAttribueeDto> notes);

    FicheEvaluationDto enregistrerNotesResponsableEntreprise(Long ficheId, Long userId, List<NoteAttribueeDto> notes);
}
