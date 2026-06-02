package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.validation.PersonName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {

    @PersonName
    @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
    private String nom;

    @PersonName
    @Size(max = 100, message = "Le prenom ne doit pas depasser 100 caracteres")
    private String prenom;

    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne doit pas depasser 150 caracteres")
    private String email;

    @Size(max = 30, message = "Le numero de telephone ne doit pas depasser 30 caracteres")
    private String telephone;

    @Size(max = 255, message = "L'adresse ne doit pas depasser 255 caracteres")
    private String adresse;

    @Size(max = 100, message = "Le poste ne doit pas depasser 100 caracteres")
    private String poste;

    @Size(max = 100, message = "Le service ne doit pas depasser 100 caracteres")
    private String service;

    @Size(max = 100, message = "La specialite ne doit pas depasser 100 caracteres")
    private String specialite;

    @Size(max = 100, message = "Le grade ne doit pas depasser 100 caracteres")
    private String grade;

    @Size(max = 100, message = "Le matricule ne doit pas depasser 100 caracteres")
    private String matricule;

    @PastOrPresent(message = "La date de naissance ne peut pas etre posterieure a la date du jour.")
    private LocalDate dateNaiss;

    private Integer niveau;

    // Pas de limite de taille stricte : la valeur peut etre une donnee Base-64 (image importee)
    // ou une URL courte. La colonne DB est en LONGTEXT.
    private String urlSignature;
}
