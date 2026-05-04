package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;

public interface ResponsableEntrepriseMapper {

    ResponsableEntrepriseDto toDto(ResponsableEntreprise entity);

    ResponsableEntreprise toEntity(ResponsableEntrepriseDto dto);
}