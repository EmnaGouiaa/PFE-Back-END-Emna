package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.validation.PersonName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRepresentantEntrepriseRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    @PersonName
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    @PersonName
    private String prenom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String motDePasse;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de téléphone invalide."
    )
    private String telephone;

    private Long entrepriseId;

    private String nomEntreprise;
    private String adresseEntreprise;
    private String secteurActivite;

    @Email(message = "Format d'email entreprise invalide.")
    private String emailEntreprise;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de téléphone entreprise invalide."
    )
    private String telephoneEntreprise;
}
