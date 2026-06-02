package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.validation.PersonName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateDemandeCreationCompteEntrepriseRequest {

    /** Optionnel pour un stagiaire connecte (resolu depuis le JWT cote service). */
    private Long stagiaireId;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire.")
    private String nomEntreprise;

    @NotBlank(message = "L'email de l'entreprise est obligatoire.")
    @Email(message = "Format d'email entreprise invalide.")
    private String emailEntreprise;

    @NotBlank(message = "Le telephone de l'entreprise est obligatoire.")
    @Pattern(regexp = "^\\+?[0-9][0-9 .()\\-]{7,19}$", message = "Format de telephone entreprise invalide.")
    private String telephoneEntreprise;

    @NotBlank(message = "L'adresse est obligatoire.")
    private String adresse;

    @NotBlank(message = "Le secteur d'activite est obligatoire.")
    private String secteurActivite;

    @NotBlank(message = "Le nom du responsable est obligatoire.")
    @PersonName
    private String nomResponsable;

    @NotBlank(message = "Le prenom du responsable est obligatoire.")
    @PersonName
    private String prenomResponsable;

    @NotBlank(message = "L'email du responsable est obligatoire.")
    @Email(message = "Format d'email responsable invalide.")
    private String emailResponsable;

    @NotBlank(message = "Le telephone du responsable est obligatoire.")
    @Pattern(regexp = "^\\+?[0-9][0-9 .()\\-]{7,19}$", message = "Format de telephone responsable invalide.")
    private String telephoneResponsable;
}
