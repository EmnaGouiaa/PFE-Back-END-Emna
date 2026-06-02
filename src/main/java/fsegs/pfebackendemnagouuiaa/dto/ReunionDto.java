package fsegs.pfebackendemnagouuiaa.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fsegs.pfebackendemnagouuiaa.configuration.LocalTimeObjectDeserializer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionDto {

    private Long id;

    private String numReunion;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "L'heure est obligatoire")
    @JsonDeserialize(using = LocalTimeObjectDeserializer.class)
    private LocalTime heure;

    private String observation;

    private String compteRendu;

    @NotNull(message = "Le stage est obligatoire")
    private Long stageId;

    private String stageTitre;
    private String stagiaireNom;
    private String entrepriseNom;
    /** Nom du responsable / tuteur entreprise rattaché au stage. */
    private String nomTuteurEntreprise;
    private String typeReunion;
    private String typeEncadrantCreateur;
    private String nomEncadrantCreateur;
    private Long encadrantCreateurId;

    private Set<Long> participantIds;
    private Set<String> participantNoms;
}
