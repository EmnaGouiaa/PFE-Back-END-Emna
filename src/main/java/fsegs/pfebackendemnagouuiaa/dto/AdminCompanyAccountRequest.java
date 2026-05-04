package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCompanyAccountRequest {

    private Long representantId;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    @Email(message = "Format d'email entreprise invalide")
    @Pattern(regexp = "^$|.*@.*", message = "L'email entreprise doit contenir @")
    private String emailEntreprise;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de telephone entreprise invalide"
    )
    private String telephoneEntreprise;

    private String adresse;
    private String secteurActivite;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    private String nomResponsable;

    @NotBlank(message = "Le prenom du responsable est obligatoire")
    private String prenomResponsable;

    @NotBlank(message = "L'email du responsable est obligatoire")
    @Email(message = "Format d'email responsable invalide")
    @Pattern(regexp = ".*@.*", message = "L'email du responsable doit contenir @")
    private String emailResponsable;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de telephone responsable invalide"
    )
    private String telephoneResponsable;
}
