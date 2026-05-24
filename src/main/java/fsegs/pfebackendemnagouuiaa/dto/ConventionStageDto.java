package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConventionStageDto {

    private Long id;
    private Integer numConv;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // ── Drapeaux de signature (calculés, backward-compatible avec le frontend) ─

    private Boolean signeeEncAca;
    private Boolean signeeEncPro;
    private Boolean signeeEntreprise;
    private Boolean signeeResp;
    private Boolean signeeStagiaire;
    private Boolean statutSignatures;

    // Champs enrichis pour le responsable universitaire (rétrocompatibilité)
    private LocalDateTime dateSignatureResponsableUniversitaire;
    private String nomResponsableUniversitaireSignataire;

    // ── Liste complète des signatures ──────────────────────────────────────────

    private List<SignatureDto> signatures = new ArrayList<>();

    // ── Relations ──────────────────────────────────────────────────────────────

    private Long stageId;
    private String stageTitre;
    private Long demandeStageId;
}
