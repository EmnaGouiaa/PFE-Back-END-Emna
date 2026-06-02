package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.DemandeCreationCompteEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;

/**
 * Contrat de conversion entre l'entité {@link DemandeCreationCompteEntreprise} et le DTO
 * {@link DemandeCreationCompteEntrepriseDto}.
 * <p>
 * Implémenté par {@link DemandeCreationCompteEntrepriseMapperImpl} (bean Spring). Prévu pour exposer les demandes
 * côté API ; {@link fsegs.pfebackendemnagouuiaa.services.DemandeCreationCompteEntrepriseServiceImpl} manipule
 * encore l'entité directement.
 */
public interface DemandeCreationCompteEntrepriseMapper {

    /**
     * Transforme une demande persistée en DTO (entreprise, responsable, stagiaire demandeur).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    DemandeCreationCompteEntrepriseDto toDto(DemandeCreationCompteEntreprise entity);

    /**
     * Construit une entité à partir du DTO (sans résolution JPA du stagiaire).
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    DemandeCreationCompteEntreprise toEntity(DemandeCreationCompteEntrepriseDto dto);
}
