package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import org.springframework.stereotype.Component;

/**
 * Mapper Spring entre {@link Stagiaire} et les DTO {@link StagiaireRequestDTO} /
 * {@link StagiaireResponseDTO}.
 * <p>
 * Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.StagiaireServiceImpl} ; le service complète
 * ensuite filière et encadrant académique non portés par le mapper à l'écriture.
 */
@Component
public class StagiaireMapper {

    /**
     * Construit une entité stagiaire à partir d'une requête (rôle {@link Role#STAGIAIRE} imposé).
     *
     * @param dto requête source ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    public Stagiaire toEntity(StagiaireRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Stagiaire stagiaire = new Stagiaire();

        stagiaire.setNom(dto.getNom());
        stagiaire.setPrenom(dto.getPrenom());
        stagiaire.setEmail(dto.getEmail());
        stagiaire.setMotDePasse(dto.getMotDePasse());
        stagiaire.setTelephone(dto.getTelephone());
        stagiaire.setActif(dto.getActif() != null ? dto.getActif() : true);
        stagiaire.setUrlSignature(dto.getUrlSignature());

        stagiaire.setMatricule(dto.getMatricule());
        stagiaire.setDateNaiss(dto.getDateNaiss());
        stagiaire.setNiveau(dto.getNiveau());

        stagiaire.setRole(Role.STAGIAIRE);

        return stagiaire;
    }

    /**
     * Transforme un stagiaire persisté en DTO de réponse (filière et encadrant académique dénormalisés).
     *
     * @param stagiaire entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    public StagiaireResponseDTO toResponseDTO(Stagiaire stagiaire) {
        if (stagiaire == null) {
            return null;
        }

        return StagiaireResponseDTO.builder()
                .id(stagiaire.getId())
                .nom(stagiaire.getNom())
                .prenom(stagiaire.getPrenom())
                .email(stagiaire.getEmail())
                .telephone(stagiaire.getTelephone())
                .adresse(stagiaire.getAdresse())
                .actif(stagiaire.getActif())
                .urlSignature(stagiaire.getUrlSignature())
                .role(stagiaire.getRole() != null ? stagiaire.getRole().name() : null)
                .matricule(stagiaire.getMatricule())
                .dateNaiss(stagiaire.getDateNaiss())
                .niveau(stagiaire.getNiveau())
                .filiereId(stagiaire.getFiliere() != null ? stagiaire.getFiliere().getId() : null)
                .filiereNom(stagiaire.getFiliere() != null ? stagiaire.getFiliere().getNom() : null)
                .encadrantAcademiqueId(stagiaire.getEncadrantAcademique() != null ? stagiaire.getEncadrantAcademique().getId() : null)
                .encadrantAcademiqueNom(stagiaire.getEncadrantAcademique() != null
                        ? (stagiaire.getEncadrantAcademique().getPrenom() + " " + stagiaire.getEncadrantAcademique().getNom()).trim()
                        : null)
                .encadrantAcademiqueEmail(stagiaire.getEncadrantAcademique() != null ? stagiaire.getEncadrantAcademique().getEmail() : null)
                .build();
    }

    /**
     * Met à jour les champs modifiables d'un stagiaire existant (sans toucher filière / encadrant).
     *
     * @param dto requête de mise à jour
     * @param stagiaire entité cible ; sortie immédiate si l'un des paramètres est {@code null}
     */
    public void updateEntityFromDto(StagiaireRequestDTO dto, Stagiaire stagiaire) {
        if (dto == null || stagiaire == null) {
            return;
        }

        stagiaire.setNom(dto.getNom());
        stagiaire.setPrenom(dto.getPrenom());
        stagiaire.setEmail(dto.getEmail());
        stagiaire.setMotDePasse(dto.getMotDePasse());
        stagiaire.setTelephone(dto.getTelephone());
        stagiaire.setActif(dto.getActif() != null ? dto.getActif() : true);
        stagiaire.setUrlSignature(dto.getUrlSignature());

        stagiaire.setMatricule(dto.getMatricule());
        stagiaire.setDateNaiss(dto.getDateNaiss());
        stagiaire.setNiveau(dto.getNiveau());

        stagiaire.setRole(Role.STAGIAIRE);
    }
}
