package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String ancienMotDePasse;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le nouveau mot de passe doit contenir entre 6 et 100 caracteres")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du nouveau mot de passe est obligatoire")
    private String confirmationNouveauMotDePasse;
}
