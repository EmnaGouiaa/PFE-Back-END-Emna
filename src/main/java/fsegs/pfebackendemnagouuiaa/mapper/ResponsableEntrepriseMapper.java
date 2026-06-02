package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;

/**
 * Contrat de conversion entre {@link ResponsableEntreprise} et {@link ResponsableEntrepriseDto}.
 * <p>
 * Implémenté par {@link ResponsableEntrepriseMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.ResponsableEntrepriseServiceImpl}.
 */
public interface ResponsableEntrepriseMapper {

    /**
     * Transforme un responsable entreprise en DTO, avec dénormalisation de l'entreprise.
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    ResponsableEntrepriseDto toDto(ResponsableEntreprise entity);

    /**
     * Construit une entité à partir du DTO ; force {@code actif = true} à la création.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    ResponsableEntreprise toEntity(ResponsableEntrepriseDto dto);
}
