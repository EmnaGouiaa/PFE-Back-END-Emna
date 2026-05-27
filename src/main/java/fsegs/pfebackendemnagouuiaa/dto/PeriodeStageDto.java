package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodeStageDto {

    private Long      id;
    private String    libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean   active;
}
