package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConventionStageDto {
    private Long id;
    private Integer numConv;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private Boolean signeeEncAca;
    private Boolean signeeEncPro;
    private Boolean signeeEntreprise;
    private Boolean signeeResp;
    private LocalDateTime dateSignatureResponsableUniversitaire;
    private String nomResponsableUniversitaireSignataire;
    private Boolean signeeStagiaire;
    private Boolean statutSignatures;

    private Long stageId;
    private String stageTitre;

    private Long demandeStageId;

}
