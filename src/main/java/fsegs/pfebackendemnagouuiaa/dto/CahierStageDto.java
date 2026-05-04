package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStageDto {

    private Long id;
    private LocalDate dateGeneration;
    private LocalDate dateSignature;

    private Boolean estSigne;
    private Boolean signeeEncAcad;
    private Boolean signeeEncPro;
    private Boolean signeeRespEntreprise;
    private Boolean signeeStagiaire;

    private Long stageId;
    private String stageTitre;
}