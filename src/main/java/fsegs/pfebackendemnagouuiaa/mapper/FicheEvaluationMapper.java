package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;

/**
 * Contrat de conversion entre l'entité {@link FicheEvaluation} et le DTO {@link FicheEvaluationDto}.
 * <p>
 * Implémenté par {@link FicheEvaluationMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.FicheEvaluationServiceImpl} pour les échanges API
 * (consultation, saisie, signatures et notes associées).
 */
public interface FicheEvaluationMapper {

    /**
     * Transforme une fiche persistée en DTO enrichi (stage, réunion finale, signatures, notes, statuts calculés).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    FicheEvaluationDto toDto(FicheEvaluation entity);

    /**
     * Construit une entité à partir du DTO ; références légères par identifiant pour stage et réunion finale.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    FicheEvaluation toEntity(FicheEvaluationDto dto);
}
