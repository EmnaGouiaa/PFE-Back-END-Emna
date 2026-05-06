package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCreationCompteEntrepriseDto {
    private Long id;

    private Long stagiaireId;
    private String stagiaireNom;

    private LocalDate dateDemande;

    private StatutValidation statutAdmin;
    private StatutValidation statutResponsableStages;

    private String nomEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String adresse;
    private String secteurActivite;

    private String nomResponsable;
    private String prenomResponsable;
    private String emailResponsable;
    private String telephoneResponsable;

    private Long valideeParAdminId;
    private Long valideeParEncadrantId;

    private Long valideeParEncadrantAcademiqueId;
    private String valideeParEncadrantAcademiqueNom;

    private LocalDateTime creeLe;
    private LocalDateTime misAJourLe;
}
