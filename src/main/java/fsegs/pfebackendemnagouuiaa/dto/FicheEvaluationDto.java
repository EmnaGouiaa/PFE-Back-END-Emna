package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FicheEvaluationDto {

    private Long id;

    // ── Partie Encadrant Professionnel ─────────────────────────────────────────

    private String pointFortEncadrantPro;
    private String axeAmeliorationEncadrantPro;

    // Champs enrichis EP (backward-compatible avec le frontend)
    private String signatureEncadrantProfessionnel;   // urlSignature du signataire
    private LocalDateTime dateSignatureEncadrantProfessionnel;
    private Long signataireEncadrantProfessionnelId;
    private String roleSignatureEncadrantProfessionnel;
    private String nomSignataireEncadrantProfessionnel;

    // ── Partie Responsable Entreprise ──────────────────────────────────────────

    private String pointFortResponsableEntreprise;
    private String axeAmeliorationResponsableEntreprise;

    // Champs enrichis RE (backward-compatible avec le frontend)
    private String signatureRepresentantEntreprise;   // urlSignature du signataire
    private LocalDateTime dateSignatureRepresentantEntreprise;
    private Long signataireRepresentantEntrepriseId;
    private String roleSignatureRepresentantEntreprise;
    private String nomSignataireRepresentantEntreprise;

    // ── Statuts calculés ───────────────────────────────────────────────────────

    private Boolean donneesCompletes;
    private Boolean signaturesCompletes;
    private Boolean complete;
    private Boolean verrouillee;

    // ── Liste complète des signatures ──────────────────────────────────────────

    private List<SignatureDto> signatures = new ArrayList<>();

    // ── Données du stage ───────────────────────────────────────────────────────

    private Long stageId;
    private String stageTitre;
    private String stageSujet;
    private LocalDate stageDateDebut;
    private LocalDate stageDateFin;
    private String stagiaireNomComplet;
    private String sectionStagiaire;
    private String entrepriseNom;
    private String entrepriseLieuStage;

    // ── Réunion finale ─────────────────────────────────────────────────────────

    private Long reunionFinaleId;
    private String reunionFinaleNumero;
    private LocalDate reunionFinaleDate;
    private LocalTime reunionFinaleHeure;

    private Double noteFinale;
    private List<NoteAttribueeDto> notesAttribuees = new ArrayList<>();
}
