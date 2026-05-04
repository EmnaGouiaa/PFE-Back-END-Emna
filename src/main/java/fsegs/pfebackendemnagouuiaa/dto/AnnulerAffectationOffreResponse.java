package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnulerAffectationOffreResponse {

    private String message;
    private Long offreId;
    private String offreStatut;
    private Long stageId;
    private String stageStatut;
    private String modeAnnulation;
}
