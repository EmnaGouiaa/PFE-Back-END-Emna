package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;

public interface ConventionStageMapper {
    ConventionStageDto toDto(ConventionStage entity);

    ConventionStage toEntity(ConventionStageDto dto);
}
