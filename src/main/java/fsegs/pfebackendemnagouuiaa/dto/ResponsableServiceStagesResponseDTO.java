package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponsableServiceStagesResponseDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private Boolean actif;
    private String nomFichierSignature;
    private String role;

    private String service;
}