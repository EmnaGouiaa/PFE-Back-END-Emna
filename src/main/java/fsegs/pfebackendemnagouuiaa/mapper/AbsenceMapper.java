package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;

/**
 * Contrat de conversion entre l'entité {@link Absence} et le DTO {@link AbsenceDto}.
 * <p>
 * Implémenté par {@link AbsenceMapperImpl}. Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.AbsenceServiceImpl}
 * pour exposer et persister les absences liées à un stage.
 */
public interface AbsenceMapper {

    /**
     * Transforme une entité persistée en DTO API, avec dénormalisation du stage associé.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO prêt pour la couche REST
     */
    AbsenceDto toDto(Absence entity);

    /**
     * Construit une entité à partir d'un DTO (sans résolution JPA du stage ; le service associe le stage).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    Absence toEntity(AbsenceDto dto);
}
