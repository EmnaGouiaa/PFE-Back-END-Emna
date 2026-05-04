package fsegs.pfebackendemnagouuiaa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncadrantProfessionnelDto {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, message = "Le nom doit contenir au moins 2 caracteres.")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire.")
    @Size(min = 2, message = "Le prenom doit contenir au moins 2 caracteres.")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format email invalide.")
    @Pattern(regexp = ".*@.*", message = "L'email doit contenir @.")
    private String email;

    @NotBlank(message = "Le numero de telephone est obligatoire.")
    @Pattern(regexp = "^\\+?[0-9][0-9 .()\\-]{7,19}$", message = "Le numero de telephone est invalide.")
    private String telephone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String motDePasse;

    private String poste;
    private String service;
    private Long entrepriseId;
    private String entrepriseNom;
}
