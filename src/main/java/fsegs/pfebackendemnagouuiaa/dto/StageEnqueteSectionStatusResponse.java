package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageEnqueteSectionStatusResponse {

    private Long stageId;
    private boolean sectionEnqueteOuverte;
    private LocalDate dateReunionFinale;
    private LocalDate dateFinStage;
    private boolean rapportDisponible;
    private String rapportNomFichier;
}
