package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link EncadrantProfessionnelMapper}.
 * <p>
 * Conversion {@link EncadrantProfessionnel} ↔ {@link EncadrantProfessionnelDto}, utilisée par
 * {@link fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelServiceImpl}.
 */
@Component
public class EncadrantProfessionnelMapperImpl implements EncadrantProfessionnelMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Expose {@code entrepriseId} et {@code entrepriseNom} lorsque la relation est chargée.
     */
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

        dto.setActif(entity.getActif());

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Valeur par défaut {@code actif = true} si le DTO ne précise pas le statut (constructeur sans @Builder.Default).
     */
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
        entity.setActif(dto.getActif() != null ? dto.getActif() : true);
        return entity;
    }
}
