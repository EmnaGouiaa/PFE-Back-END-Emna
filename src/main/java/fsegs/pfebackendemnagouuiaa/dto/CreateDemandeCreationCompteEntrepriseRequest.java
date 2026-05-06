package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

@Data
public class CreateDemandeCreationCompteEntrepriseRequest {

    private Long stagiaireId;

    private String nomEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String adresse;
    private String secteurActivite;

    private String nomResponsable;
    private String prenomResponsable;
    private String emailResponsable;
    private String telephoneResponsable;
}
