package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;

public interface ReunionHebdomadaireMapper {

    ReunionHebdomadaireDto toDto(ReunionHebdomadaire entity);

    ReunionHebdomadaire toEntity(ReunionHebdomadaireDto dto);
}