package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.mapper.ResponsableEntrepriseMapper;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link ResponsableEntrepriseMapper}.
 * <p>
 * Utilisée par {@link fsegs.pfebackendemnagouuiaa.services.ResponsableEntrepriseServiceImpl}.
 */
@Component
public class ResponsableEntrepriseMapperImpl implements ResponsableEntrepriseMapper {

    /** {@inheritDoc} */
    @Override
    public ResponsableEntrepriseDto toDto(ResponsableEntreprise entity) {
        if (entity == null) {
            return null;
        }

        ResponsableEntrepriseDto dto = new ResponsableEntrepriseDto();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setPoste(entity.getPoste());
        dto.setService(entity.getService());

        if (entity.getEntreprise() != null) {
            dto.setEntrepriseId(entity.getEntreprise().getId());
            dto.setEntrepriseNom(entity.getEntreprise().getNom());
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@code actif} est fixé à {@code true} : le constructeur vide n'applique pas la valeur @Builder.Default.
     */
    @Override
    public ResponsableEntreprise toEntity(ResponsableEntrepriseDto dto) {
        if (dto == null) {
            return null;
        }

        ResponsableEntreprise entity = new ResponsableEntreprise();
        entity.setId(dto.getId());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setPoste(dto.getPoste());
        entity.setService(dto.getService());
        entity.setActif(true);
        return entity;
    }
}
