package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;

public interface NoteAttribueeMapper {

    NoteAttribueeDto toDto(NoteAttribuee entity);

    NoteAttribuee toEntity(NoteAttribueeDto dto);
}