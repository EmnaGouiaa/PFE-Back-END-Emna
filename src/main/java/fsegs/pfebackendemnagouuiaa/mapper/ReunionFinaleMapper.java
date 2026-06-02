package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;

/**
 * Contrat de conversion entre {@link ReunionFinale} et {@link ReunionFinaleDto}.
 * <p>
 * Implémenté par {@link ReunionFinaleMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.ReunionFinaleServiceImpl}.
 */
public interface ReunionFinaleMapper {

    /**
     * Transforme une réunion finale en DTO, avec repli sur l'encadrant professionnel du stage si créateur absent.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    ReunionFinaleDto toDto(ReunionFinale entity);

    /**
     * Construit une entité à partir du DTO (champs scalaires uniquement).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    ReunionFinale toEntity(ReunionFinaleDto dto);
}
