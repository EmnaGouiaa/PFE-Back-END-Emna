package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;

/**
 * Contrat de conversion entre l'entité {@link Entreprise} et le DTO {@link EntrepriseDto}.
 * <p>
 * Implémenté par {@link EntrepriseMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.EntrepriseServiceImpl}.
 */
public interface EntrepriseMapper {

    /**
     * Transforme une entreprise persistée en DTO.
     *
     * @param entreprise entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    EntrepriseDto toDto(Entreprise entreprise);

    /**
     * Construit une entité à partir du DTO.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    Entreprise toEntity(EntrepriseDto dto);
}
