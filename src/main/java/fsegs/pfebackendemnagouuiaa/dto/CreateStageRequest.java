package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Corps JSON de création ou mise à jour d'un {@link fsegs.pfebackendemnagouuiaa.entities.Stage}.
 * <p>
 * Utilisé par {@code POST /api/stages} et {@code PUT /api/stages/{id}}
 * ({@link fsegs.pfebackendemnagouuiaa.controller.StageController}).
 * Les identifiants référencent les entités liées (stagiaire, entreprise, encadrants, offre).
 * </p>
 */
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