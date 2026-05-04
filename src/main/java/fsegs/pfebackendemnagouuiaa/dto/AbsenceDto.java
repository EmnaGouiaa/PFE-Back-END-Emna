package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceDto {

    private Long id;
    private LocalDate dateAbsence;
    private Integer nbAbsence;
    private String justification;
    private String commentaire;
    private String statut;

    private Long stageId;
    private String stageTitre;
}
