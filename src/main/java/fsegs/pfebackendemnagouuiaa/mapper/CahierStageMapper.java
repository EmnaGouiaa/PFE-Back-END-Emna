package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;

/**
 * Contrat de conversion entre l'entité {@link CahierStage} et le DTO {@link CahierStageDto}.
 * <p>
 * Implémenté par {@link CahierStageMapperImpl}. Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.CahierStageServiceImpl}
 * pour la génération, la consultation et la signature du cahier de stage.
 */
public interface CahierStageMapper {

    /**
     * Transforme un cahier persisté en DTO enrichi (signatures, indicateurs de complétude, stage).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    CahierStageDto toDto(CahierStage entity);

    /**
     * Construit une entité minimale à partir du DTO (métadonnées de dates et identifiant).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité sans relations ni collection de signatures
     */
    CahierStage toEntity(CahierStageDto dto);
}
