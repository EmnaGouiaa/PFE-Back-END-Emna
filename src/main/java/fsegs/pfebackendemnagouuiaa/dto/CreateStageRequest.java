package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateStageRequest {
    private String titre;
    private String sujet;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer duree;
    private Integer nbSemaine;
    private String niveauSouhaite;

    private Long stagiaireId;
    private Long entrepriseId;
    private Long encadrantAcademiqueId;
    private Long encadrantProfessionnelId;
    private Long tuteurEntrepriseId;
    private Long offreSourceId;
}