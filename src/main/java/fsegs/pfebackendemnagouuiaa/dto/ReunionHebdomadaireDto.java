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
    private String stagiaireNom;
    private String entrepriseNom;
    private String nomTuteurEntreprise;
    private String typeReunion;
    private String typeEncadrantCreateur;
    private String nomEncadrantCreateur;
    private Long encadrantCreateurId;

    private Set<Long> participantIds;
    private Set<String> participantNoms;

    private Long cahierStageId;
}
