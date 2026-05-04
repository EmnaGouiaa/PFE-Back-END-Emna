package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import org.springframework.stereotype.Component;

@Component
public class EncadrantAcademiqueMapperImpl implements EncadrantAcademiqueMapper {

    @Override
    public EncadrantAcademiqueDto toDto(EncadrantAcademique entity) {
        if (entity == null)
            return null;

        return EncadrantAcademiqueDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .email(entity.getEmail())
                .telephone(entity.getTelephone())
                .grade(entity.getGrade())
                .matricule(entity.getMatricule())
                .specialite(entity.getSpecialite())
                .build();
    }

    @Override
    public EncadrantAcademique toEntity(EncadrantAcademiqueDto dto) {
        if (dto == null)
            return null;

        return EncadrantAcademique.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .grade(dto.getGrade())
                .matricule(dto.getMatricule())
                .specialite(dto.getSpecialite())
                .build();
    }
}