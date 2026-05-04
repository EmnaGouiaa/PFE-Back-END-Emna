package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.NotBlank;
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
    private LocalTime heure;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    private String compteRendu;

    @NotNull(message = "Le stage est obligatoire")
    private Long stageId;

    private String stageTitre;
    private String stagiaireNom;
    private String entrepriseNom;
    private String typeReunion;

    private Set<Long> participantIds;
}
