package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import org.springframework.stereotype.Component;

@Component
public class EncadrantProfessionnelMapperImpl implements EncadrantProfessionnelMapper {

    @Override
    public EncadrantProfessionnelDto toDto(EncadrantProfessionnel entity) {
        if (entity == null) {
            return null;
        }

        EncadrantProfessionnelDto dto = new EncadrantProfessionnelDto();
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
    public EncadrantProfessionnel toEntity(EncadrantProfessionnelDto dto) {
        if (dto == null) {
            return null;
        }

        EncadrantProfessionnel entity = new EncadrantProfessionnel();
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
