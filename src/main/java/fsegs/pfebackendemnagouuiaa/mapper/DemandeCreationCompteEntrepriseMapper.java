package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.DemandeCreationCompteEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;

public interface DemandeCreationCompteEntrepriseMapper {
    DemandeCreationCompteEntrepriseDto toDto(DemandeCreationCompteEntreprise entity);

    DemandeCreationCompteEntreprise toEntity(DemandeCreationCompteEntrepriseDto dto);
}
