package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompanyAccountResponse {
    private Long entrepriseId;
    private String nomEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String adresse;
    private String secteurActivite;

    private Long representantId;
    private String nomResponsable;
    private String prenomResponsable;
    private String emailResponsable;
    private String telephoneResponsable;
    private Boolean emailSent;
    private String message;
}
