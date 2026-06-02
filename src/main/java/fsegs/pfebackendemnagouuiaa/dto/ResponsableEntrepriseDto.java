package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.validation.PersonName;
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
public class ResponsableEntrepriseDto {
    private Long id;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    @PersonName
    private String nom;
    @NotBlank(message = "Le prenom du responsable est obligatoire")
    @PersonName
    private String prenom;

    @NotBlank(message = "L'email du responsable est obligatoire")
    @Email(message = "Format d'email responsable invalide")
    @Pattern(regexp = ".*@.*", message = "L'email du responsable doit contenir @")
    private String email;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9 .()\\-]{7,19}$",
            message = "Format de telephone responsable invalide"
    )
    private String telephone;

    private String poste;
    private String service;

    private Long entrepriseId;
    private String entrepriseNom;
}
