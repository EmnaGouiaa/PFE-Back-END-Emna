package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnqueteSatisfactionDto {

    private Long reunionFinaleId;
    private Long stageId;
    private String stageTitre;
    private Long enqueteId;
    private String titre;
    private String description;
    private String urlFormulaire;
    private String statut;
    private Boolean disponible;
    private Boolean dateAtteinte;
    private String message;
    private Boolean sectionEnqueteOuverte;

    /** Champ renvoyé par GET /api/enquete — indique si l'enquête est active. */
    private Boolean active;

    /** Champ renvoyé par GET /api/enquete — date de dernière modification ISO-8601. */
    private String dateModification;
}
