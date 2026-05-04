package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;

public interface CritereEvaluationMapper {
    CritereEvaluationDto toDto(CritereEvaluation entity);
    CritereEvaluation toEntity(CritereEvaluationDto dto);
}