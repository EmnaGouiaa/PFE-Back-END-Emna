package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;

public interface CahierStageMapper {

    CahierStageDto toDto(CahierStage entity);

    CahierStage toEntity(CahierStageDto dto);
}