package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;

public interface EncadrantProfessionnelMapper {
    EncadrantProfessionnelDto toDto(EncadrantProfessionnel entity);

    EncadrantProfessionnel toEntity(EncadrantProfessionnelDto dto);
}
