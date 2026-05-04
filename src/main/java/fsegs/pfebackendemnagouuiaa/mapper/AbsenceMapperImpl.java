package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.mapper.AbsenceMapper;
import org.springframework.stereotype.Component;

@Component
public class AbsenceMapperImpl implements AbsenceMapper {

    @Override
    public AbsenceDto toDto(Absence entity) {
        if (entity == null) {
            return null;
        }

        AbsenceDto dto = new AbsenceDto();
        dto.setId(entity.getId());
        dto.setDateAbsence(entity.getDateAbsence());
        dto.setNbAbsence(entity.getNbAbsence());
        dto.setJustification(entity.getJustification());
        dto.setCommentaire(entity.getCommentaire());
        dto.setStatut(entity.getStatut());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        return dto;
    }

    @Override
    public Absence toEntity(AbsenceDto dto) {
        if (dto == null) {
            return null;
        }

        Absence entity = new Absence();
        entity.setId(dto.getId());
        entity.setDateAbsence(dto.getDateAbsence());
        entity.setNbAbsence(dto.getNbAbsence());
        entity.setJustification(dto.getJustification());
        entity.setCommentaire(dto.getCommentaire());
        entity.setStatut(dto.getStatut());

        return entity;
    }
}
