package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;

import java.util.List;

public interface CritereEvaluationService {

    CritereEvaluationDto create(CritereEvaluationDto dto);

    CritereEvaluationDto getById(Long id);

    List<CritereEvaluationDto> getAll();

    List<CritereEvaluationDto> getByFicheId(Long ficheId);

    CritereEvaluationDto update(Long id, CritereEvaluationDto dto);

    void delete(Long id);
}