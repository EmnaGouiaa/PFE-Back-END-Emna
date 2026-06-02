package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import org.springframework.stereotype.Component;

/**
 * Mapper Spring (interface + implémentation unique) entre {@link ResponsableServiceStages} et les DTO
 * {@link ResponsableServiceStagesRequestDTO} / {@link ResponsableServiceStagesResponseDTO}.
 * <p>
 * Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.ResponsableServiceStagesServiceImpl} pour la création,
 * la mise à jour et l'exposition des responsables du service des stages.
 */
@Component
public class ResponsableServiceStagesMapper {

    /**
     * Crée une nouvelle entité à partir d'une requête de création.
     *
     * @param dto requête source ; {@code null} renvoie {@code null}
     * @return entité non persistée avec rôle {@link Role#RESPONSABLE_STAGE}
     */
    public ResponsableServiceStages toEntity(ResponsableServiceStagesRequestDTO dto) {
        if (dto == null) return null;

        ResponsableServiceStages responsable = new ResponsableServiceStages();
        mapCommonFields(dto, responsable);
        return responsable;
    }

    /**
     * Met à jour une entité existante à partir du DTO (sans recréer l'identifiant).
     *
     * @param dto requête de mise à jour
     * @param responsable entité cible ; sortie immédiate si {@code dto} ou {@code responsable} est {@code null}
     */
    public void updateEntityFromDto(ResponsableServiceStagesRequestDTO dto, ResponsableServiceStages responsable) {
        if (dto == null || responsable == null) return;
        mapCommonFields(dto, responsable);
    }

    /**
     * Champs communs création / mise à jour : mot de passe, service par défaut, rôle fixe.
     */
    private void mapCommonFields(ResponsableServiceStagesRequestDTO dto, ResponsableServiceStages responsable) {
        responsable.setNom(dto.getNom());
        responsable.setPrenom(dto.getPrenom());
        responsable.setEmail(dto.getEmail());
        responsable.setMotDePasse(dto.getMotDePasse());
        responsable.setTelephone(dto.getTelephone());
        responsable.setActif(dto.getActif() != null ? dto.getActif() : true);
        responsable.setUrlSignature(dto.getUrlSignature());
        // Service obligatoire métier : valeur par défaut si absent ou vide
        String service = dto.getService() != null && !dto.getService().isBlank() ? dto.getService().trim() : "Service des stages";
        responsable.setService(service);
        responsable.setRole(Role.RESPONSABLE_STAGE);
    }

    /**
     * Transforme l'entité persistée en DTO de réponse (rôle exposé en chaîne).
     *
     * @param responsable entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    public ResponsableServiceStagesResponseDTO toResponseDTO(ResponsableServiceStages responsable) {
        if (responsable == null) return null;

        return ResponsableServiceStagesResponseDTO.builder()
                .id(responsable.getId())
                .nom(responsable.getNom())
                .prenom(responsable.getPrenom())
                .email(responsable.getEmail())
                .telephone(responsable.getTelephone())
                .actif(responsable.getActif())
                .urlSignature(responsable.getUrlSignature())
                .role(responsable.getRole() != null ? responsable.getRole().name() : null)
                .service(responsable.getService())
                .build();
    }
}
