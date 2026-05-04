package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;

public interface EncadrantAcademiqueMapper {

    EncadrantAcademiqueDto toDto(EncadrantAcademique entity);

    EncadrantAcademique toEntity(EncadrantAcademiqueDto dto);
}