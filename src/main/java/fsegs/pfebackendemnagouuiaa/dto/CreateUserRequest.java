package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Pattern(regexp = ".*@.*", message = "L'email doit contenir @")
    private String email;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de telephone invalide"
    )
    private String telephone;

    private String matricule;

    private Boolean actif;

    private String urlSignature;

    /**
     * Identifiant de la filiere du stagiaire.
     * Obligatoire lors de la creation d'un compte STAGIAIRE.
     * Ignore pour les autres roles.
     */
    private Long filiereId;

    /**
     * Niveau du stagiaire. Saisi uniquement par l'administrateur lors de la création
     * d'un compte STAGIAIRE. Ignoré pour les autres rôles.
     */
    private Integer niveau;

    @NotNull(message = "Le role est obligatoire")
    private Role role;
}
