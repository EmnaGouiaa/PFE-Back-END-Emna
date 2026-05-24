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
public class UpdateUserRequest {

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

    @NotNull(message = "Le statut actif est obligatoire")
    private Boolean actif;

    private String urlSignature;

    @NotNull(message = "Le role est obligatoire")
    private Role role;

    /**
     * Identifiant de la filiere du stagiaire — modifiable par l'administrateur.
     * Ignore pour les autres roles. Null pour conserver la valeur existante.
     */
    private Long filiereId;

    /**
     * Niveau d'etude du stagiaire — modifiable par l'administrateur.
     * Ignore pour les autres roles. Null pour conserver la valeur existante.
     */
    private Integer niveau;
}
