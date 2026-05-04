package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    @Pattern(regexp = ".*@.*", message = "L'email doit contenir @.")
    private String email;
}
