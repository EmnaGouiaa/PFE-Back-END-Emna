package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.mapper.ResponsableEntrepriseMapper;
import org.springframework.stereotype.Component;

@Component
public class ResponsableEntrepriseMapperImpl implements ResponsableEntrepriseMapper {

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
        return entity;
    }
}