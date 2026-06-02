package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;

/**
 * Contrat de conversion entre l'entité {@link ConventionStage} et le DTO {@link ConventionStageDto}.
 * <p>
 * Implémenté par {@link ConventionStageMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.ConventionStageServiceImpl} pour le cycle de vie et la signature
 * des conventions de stage.
 */
public interface ConventionStageMapper {

    /**
     * Transforme une convention persistée en DTO (signatures, statuts, liens stage et demande).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    ConventionStageDto toDto(ConventionStage entity);

    /**
     * Construit une entité à partir des champs métier du DTO (sans relations JPA complètes).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    ConventionStage toEntity(ConventionStageDto dto);
}
