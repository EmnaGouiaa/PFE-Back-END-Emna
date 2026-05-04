package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    @Pattern(regexp = ".*@.*", message = "L'email doit contenir @.")
    private String email;

    @NotBlank(message = "Le code de verification est obligatoire.")
    @Pattern(regexp = "^\\d{6}$", message = "Le code de verification doit contenir 6 chiffres.")
    private String code;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 8, max = 100, message = "Le nouveau mot de passe doit contenir entre 8 et 100 caracteres.")
    private String newPassword;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
    @Size(min = 8, max = 100, message = "La confirmation du mot de passe doit contenir entre 8 et 100 caracteres.")
    private String confirmPassword;
}
