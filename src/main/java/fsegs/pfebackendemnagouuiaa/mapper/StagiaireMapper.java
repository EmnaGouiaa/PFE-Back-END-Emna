package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import org.springframework.stereotype.Component;

@Component
public class StagiaireMapper {
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
