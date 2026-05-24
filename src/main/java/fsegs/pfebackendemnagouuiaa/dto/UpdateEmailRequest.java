package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmailRequest {

    @NotBlank(message = "La nouvelle adresse e-mail est obligatoire.")
    @Email(message = "Format d'e-mail invalide.")
    @Size(max = 150, message = "L'adresse e-mail ne doit pas dépasser 150 caractéres.")
    private String email;

    @NotBlank(message = "Le mot de passe actuel est obligatoire.")
    private String motDePasseActuel;
}
