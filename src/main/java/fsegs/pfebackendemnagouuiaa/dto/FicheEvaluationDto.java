package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FicheEvaluationDto {

    private Long id;

    // Partie Encadrant Professionnel
    private String pointFortEncadrantPro;
    private String axeAmeliorationEncadrantPro;
    private String signatureEncadrantProfessionnel;
    private LocalDateTime dateSignatureEncadrantProfessionnel;

    private Boolean donneesCompletes;
    // Partie Responsable Entreprise
    private String pointFortResponsableEntreprise;
    private String axeAmeliorationResponsableEntreprise;
    private String signatureRepresentantEntreprise;
    private LocalDateTime dateSignatureRepresentantEntreprise;

    private Long stageId;
    private String stageTitre;

    private Long reunionFinaleId;

    private Double noteFinale;
    private Boolean signaturesCompletes;
    private Boolean complete;
    private Boolean verrouillee;
}