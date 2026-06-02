package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.mapper.AbsenceMapper;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link AbsenceMapper}.
 * <p>
 * Conversion bidirectionnelle {@link Absence} ↔ {@link AbsenceDto}, consommée par
 * {@link fsegs.pfebackendemnagouuiaa.services.AbsenceServiceImpl}.
 */
@Component
public class AbsenceMapperImpl implements AbsenceMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Enrichit le DTO avec {@code stageId} et {@code stageTitre} lorsque la relation {@code stage} est chargée.
     */
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

        // Dénormalisation lecture seule : pas de navigation inverse vers Stage dans toEntity
        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ne mappe pas {@code stageId} : l'association au stage est gérée explicitement dans le service.
     */
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
