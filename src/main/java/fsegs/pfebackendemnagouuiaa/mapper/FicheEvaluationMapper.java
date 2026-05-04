package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;

public interface FicheEvaluationMapper {
    FicheEvaluationDto toDto(FicheEvaluation entity);
    FicheEvaluation toEntity(FicheEvaluationDto dto);
}