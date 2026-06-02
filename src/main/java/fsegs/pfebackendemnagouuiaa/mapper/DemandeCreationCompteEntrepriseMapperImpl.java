package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.DemandeCreationCompteEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.mapper.DemandeCreationCompteEntrepriseMapper;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link DemandeCreationCompteEntrepriseMapper}.
 * <p>
 * Conversion {@link DemandeCreationCompteEntreprise} ↔ {@link DemandeCreationCompteEntrepriseDto}.
 * Bean disponible pour une future exposition REST ; le service métier retourne encore l'entité.
 */
@Component
public class DemandeCreationCompteEntrepriseMapperImpl implements DemandeCreationCompteEntrepriseMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Concatène nom et prénom du stagiaire pour l'affichage ({@code stagiaireNom}).
     */
    @Override
    public DemandeCreationCompteEntrepriseDto toDto(DemandeCreationCompteEntreprise entity) {
        if (entity == null) {
            return null;
        }

        DemandeCreationCompteEntrepriseDto dto = new DemandeCreationCompteEntrepriseDto();
        dto.setId(entity.getId());
        dto.setDateDemande(entity.getDateDemande());
        dto.setStatutResponsableStages(entity.getStatutResponsableStages());
        dto.setCommentaireResponsableStages(entity.getCommentaireResponsableStages());

        dto.setNomEntreprise(entity.getNomEntreprise());
        dto.setEmailEntreprise(entity.getEmailEntreprise());
        dto.setTelephoneEntreprise(entity.getTelephoneEntreprise());
        dto.setAdresse(entity.getAdresse());
        dto.setSecteurActivite(entity.getSecteurActivite());

        dto.setNomResponsable(entity.getNomResponsable());
        dto.setPrenomResponsable(entity.getPrenomResponsable());
        dto.setEmailResponsable(entity.getEmailResponsable());
        dto.setTelephoneResponsable(entity.getTelephoneResponsable());

        dto.setCreeLe(entity.getCreeLe());
        dto.setMisAJourLe(entity.getMisAJourLe());

        if (entity.getStagiaire() != null) {
            dto.setStagiaireId(entity.getStagiaire().getId());
            // Libellé affiché : nom puis prénom (ordre fixe côté API)
            dto.setStagiaireNom(entity.getStagiaire().getNom() + " " + entity.getStagiaire().getPrenom());
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ne résout pas la relation {@code stagiaire} : elle est gérée par le service à la création.
     */
    @Override
    public DemandeCreationCompteEntreprise toEntity(DemandeCreationCompteEntrepriseDto dto) {
        if (dto == null) {
            return null;
        }

        DemandeCreationCompteEntreprise entity = new DemandeCreationCompteEntreprise();
        entity.setId(dto.getId());
        entity.setDateDemande(dto.getDateDemande());
        entity.setStatutResponsableStages(dto.getStatutResponsableStages());
        entity.setCommentaireResponsableStages(dto.getCommentaireResponsableStages());

        entity.setNomEntreprise(dto.getNomEntreprise());
        entity.setEmailEntreprise(dto.getEmailEntreprise());
        entity.setTelephoneEntreprise(dto.getTelephoneEntreprise());
        entity.setAdresse(dto.getAdresse());
        entity.setSecteurActivite(dto.getSecteurActivite());

        entity.setNomResponsable(dto.getNomResponsable());
        entity.setPrenomResponsable(dto.getPrenomResponsable());
        entity.setEmailResponsable(dto.getEmailResponsable());
        entity.setTelephoneResponsable(dto.getTelephoneResponsable());

        entity.setCreeLe(dto.getCreeLe());
        entity.setMisAJourLe(dto.getMisAJourLe());

        return entity;
    }
}
