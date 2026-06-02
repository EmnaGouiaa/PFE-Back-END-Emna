package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;

/**
 * Contrat de conversion entre l'entité {@link EncadrantProfessionnel} et le DTO
 * {@link EncadrantProfessionnelDto}.
 * <p>
 * Implémenté par {@link EncadrantProfessionnelMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelServiceImpl}.
 */
public interface EncadrantProfessionnelMapper {

    /**
     * Transforme un encadrant professionnel en DTO, avec dénormalisation de l'entreprise.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    EncadrantProfessionnelDto toDto(EncadrantProfessionnel entity);

    /**
     * Construit une entité à partir du DTO (sans liaison JPA à l'entreprise).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    EncadrantProfessionnel toEntity(EncadrantProfessionnelDto dto);
}
