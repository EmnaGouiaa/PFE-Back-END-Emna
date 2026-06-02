package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;

/**
 * Contrat de conversion polymorphe entre {@link Reunion} (héritage hebdomadaire / finale) et
 * {@link ReunionDto}.
 * <p>
 * Implémenté par {@link ReunionMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.ReunionServiceImpl} pour les listes et détails unifiés.
 */
public interface ReunionMapper {

    /**
     * Transforme une réunion (sous-type inclus) en DTO générique avec type déduit et participants.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    ReunionDto toDto(Reunion entity);

    /**
     * Construit une entité à partir du DTO ; instancie par défaut une {@link fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire}.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    Reunion toEntity(ReunionDto dto);
}
