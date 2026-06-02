package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;

/**
 * Contrat de conversion entre l'entité {@link CritereEvaluation} et le DTO {@link CritereEvaluationDto}.
 * <p>
 * Implémenté par {@link CritereEvaluationMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.CritereEvaluationServiceImpl} pour administrer les critères
 * rattachés à une fiche d'évaluation.
 */
public interface CritereEvaluationMapper {

    /**
     * Transforme un critère persisté en DTO, avec identifiant de fiche parente.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    CritereEvaluationDto toDto(CritereEvaluation entity);

    /**
     * Construit une entité à partir du DTO ; la fiche est référencée par identifiant uniquement.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    CritereEvaluation toEntity(CritereEvaluationDto dto);
}
