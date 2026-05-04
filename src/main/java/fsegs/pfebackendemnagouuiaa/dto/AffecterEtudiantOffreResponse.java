package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AffecterEtudiantOffreResponse {

    private String message;
    private Long offreId;
    private String offreStatut;
    private Long stageId;
    private String stageTitre;
    private Long stagiaireId;
    private String stagiaireEmail;
}
