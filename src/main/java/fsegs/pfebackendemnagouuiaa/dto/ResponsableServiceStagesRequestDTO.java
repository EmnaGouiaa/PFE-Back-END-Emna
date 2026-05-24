package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

@Data
public class ResponsableServiceStagesRequestDTO {

    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String telephone;
    private Boolean actif;
    private String urlSignature;

    private String service;
}