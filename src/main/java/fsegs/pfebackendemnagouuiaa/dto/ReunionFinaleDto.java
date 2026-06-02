package fsegs.pfebackendemnagouuiaa.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fsegs.pfebackendemnagouuiaa.configuration.LocalTimeObjectDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionFinaleDto {

    private Long id;
    private String typeReunion;
    private String numReunion;
    private LocalDate date;

    @JsonDeserialize(using = LocalTimeObjectDeserializer.class)
    private LocalTime heure;

    private String observation;
    private String compteRendu;

    private Long stageId;
    private String stageTitre;
    private String stagiaireNom;
    private String entrepriseNom;
    private String nomTuteurEntreprise;
    private String typeEncadrantCreateur;
    private String nomEncadrantCreateur;
    private Long encadrantCreateurId;

    private Set<Long> participantIds;
}
