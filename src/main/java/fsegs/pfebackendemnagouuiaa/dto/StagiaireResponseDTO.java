package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;


import java.time.LocalDate;
@Data
@Builder(toBuilder = true)
public class StagiaireResponseDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private Boolean actif;
    private String urlSignature;
    private String role;

    private String matricule;
    private LocalDate dateNaiss;
    private Long filiereId;
    private String filiereNom;
    private Integer niveau;
    private Long encadrantAcademiqueId;
    private String encadrantAcademiqueNom;
    private String encadrantAcademiqueEmail;
    private Long stageActifId;
    private String stageActifTitre;
    private String stageActifStatut;
    private Boolean hasStageActif;
}
