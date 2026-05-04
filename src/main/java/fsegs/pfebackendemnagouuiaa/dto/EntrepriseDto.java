package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntrepriseDto {
    private Long id;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nom;
    private String adresse;

    @Email(message = "Format d'email entreprise invalide")
    @Pattern(regexp = "^$|.*@.*", message = "L'email entreprise doit contenir @")
    private String email;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de telephone entreprise invalide"
    )
    private String telephone;
    private String secteurActivite;
}
