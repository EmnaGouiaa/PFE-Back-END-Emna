package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionHebdomadaireDto {

    private Long id;
    private String numReunion;
    private LocalDate date;
    private LocalTime heure;
    private String observation;
    private String compteRendu;

    private Long stageId;
    private String stageTitre;

    private Set<Long> participantIds;

    private Integer numSemaine;
    private String objectifsSemaineProchaine;

    private Long cahierStageId;
}