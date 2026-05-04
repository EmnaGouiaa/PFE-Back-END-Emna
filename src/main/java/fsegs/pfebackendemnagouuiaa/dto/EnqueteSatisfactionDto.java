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
    private boolean disponible;
    private boolean dateAtteinte;
    private String message;
}
