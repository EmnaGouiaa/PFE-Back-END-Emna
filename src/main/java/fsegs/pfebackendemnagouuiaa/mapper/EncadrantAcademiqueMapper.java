package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;

/**
 * Contrat de conversion entre l'entité {@link EncadrantAcademique} et le DTO {@link EncadrantAcademiqueDto}.
 * <p>
 * Implémenté par {@link EncadrantAcademiqueMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.EncadrantAcademiqueServiceImpl}.
 */
public interface EncadrantAcademiqueMapper {

    /**
     * Transforme un encadrant académique persisté en DTO.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    EncadrantAcademiqueDto toDto(EncadrantAcademique entity);

    /**
     * Construit une entité à partir du DTO (mapping direct des champs métier).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    EncadrantAcademique toEntity(EncadrantAcademiqueDto dto);
}
