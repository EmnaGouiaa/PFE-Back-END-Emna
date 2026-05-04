package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;

public interface ReunionMapper {

    ReunionDto toDto(Reunion entity);

    Reunion toEntity(ReunionDto dto);
}