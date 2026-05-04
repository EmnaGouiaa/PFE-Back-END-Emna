package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AffectationEncadrantAcademiqueResponse {
    private String message;
    private StagiaireResponseDTO stagiaire;
}
