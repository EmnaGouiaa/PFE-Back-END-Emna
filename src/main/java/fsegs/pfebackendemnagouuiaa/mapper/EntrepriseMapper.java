package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;

public interface EntrepriseMapper {

    EntrepriseDto toDto(Entreprise entreprise);

    Entreprise toEntity(EntrepriseDto dto);
}