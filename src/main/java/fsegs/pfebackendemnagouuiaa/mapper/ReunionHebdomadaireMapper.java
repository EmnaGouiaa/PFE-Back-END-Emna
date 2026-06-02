package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;

/**
 * Contrat de conversion entre {@link ReunionHebdomadaire} et {@link ReunionHebdomadaireDto}.
 * <p>
 * Implémenté par {@link ReunionHebdomadaireMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.ReunionHebdomadaireServiceImpl}.
 */
public interface ReunionHebdomadaireMapper {

    /**
     * Transforme une réunion hebdomadaire en DTO (stage, participants, lien cahier de stage).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    ReunionHebdomadaireDto toDto(ReunionHebdomadaire entity);

    /**
     * Construit une entité à partir du DTO (champs scalaires uniquement).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    ReunionHebdomadaire toEntity(ReunionHebdomadaireDto dto);
}
