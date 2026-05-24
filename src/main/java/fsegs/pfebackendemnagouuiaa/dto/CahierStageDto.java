package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStageDto {

    private Long id;
    private LocalDate dateGeneration;
    private LocalDate dateSignature;

    // ── Drapeaux de signature (calculés, backward-compatible avec le frontend) ─

    private Boolean estSigne;
    private Boolean signeeEncAcad;
    private Boolean signeeEncPro;
    private Boolean signeeRespEntreprise;
    private Boolean signeeStagiaire;

    // ── Liste complète des signatures ──────────────────────────────────────────

    private List<SignatureDto> signatures = new ArrayList<>();

    // ── Relations ──────────────────────────────────────────────────────────────

    private Long stageId;
    private String stageTitre;
}
