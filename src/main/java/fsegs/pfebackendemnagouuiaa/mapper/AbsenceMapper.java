package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;

public interface AbsenceMapper {

    AbsenceDto toDto(Absence entity);

    Absence toEntity(AbsenceDto dto);
}