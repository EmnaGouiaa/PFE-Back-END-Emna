package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import org.springframework.stereotype.Component;

@Component
public class ResponsableServiceStagesMapper {

    public ResponsableServiceStages toEntity(ResponsableServiceStagesRequestDTO dto) {
        if (dto == null) return null;

        ResponsableServiceStages responsable = new ResponsableServiceStages();
        mapCommonFields(dto, responsable);
        return responsable;
    }

    public void updateEntityFromDto(ResponsableServiceStagesRequestDTO dto, ResponsableServiceStages responsable) {
        if (dto == null || responsable == null) return;
        mapCommonFields(dto, responsable);
    }

    private void mapCommonFields(ResponsableServiceStagesRequestDTO dto, ResponsableServiceStages responsable) {
        responsable.setNom(dto.getNom());
        responsable.setPrenom(dto.getPrenom());
        responsable.setEmail(dto.getEmail());
        responsable.setMotDePasse(dto.getMotDePasse());
        responsable.setTelephone(dto.getTelephone());
        responsable.setActif(dto.getActif() != null ? dto.getActif() : true);
        responsable.setUrlSignature(dto.getUrlSignature());
        String service = dto.getService() != null && !dto.getService().isBlank() ? dto.getService().trim() : "Service des stages";
        responsable.setService(service);
        responsable.setRole(Role.RESPONSABLE_STAGE);
    }

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