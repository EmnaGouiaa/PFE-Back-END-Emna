package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;

public interface ReunionFinaleMapper {

    ReunionFinaleDto toDto(ReunionFinale entity);

    ReunionFinale toEntity(ReunionFinaleDto dto);
}