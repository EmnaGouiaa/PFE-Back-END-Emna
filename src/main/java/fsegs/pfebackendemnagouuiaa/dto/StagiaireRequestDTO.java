package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.validation.PersonName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StagiaireRequestDTO {

    @NotBlank(message = "Le nom est obligatoire.")
    @PersonName
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire.")
    @PersonName
    private String prenom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    private String email;

    private String motDePasse;
    private String telephone;
    private Boolean actif;
    private String urlSignature;

    private String matricule;

    @PastOrPresent(message = "La date de naissance ne peut pas etre posterieure a la date du jour.")
    private LocalDate dateNaiss;

    private Long filiereId;
    private Integer niveau;
}
