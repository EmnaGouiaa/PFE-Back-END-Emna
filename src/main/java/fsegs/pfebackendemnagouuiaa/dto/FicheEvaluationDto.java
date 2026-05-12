package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
    private Long signataireEncadrantProfessionnelId;
    private String roleSignatureEncadrantProfessionnel;
    private String nomSignataireEncadrantProfessionnel;

    private Boolean donneesCompletes;
    // Partie Responsable Entreprise
    private String pointFortResponsableEntreprise;
    private String axeAmeliorationResponsableEntreprise;
    private String signatureRepresentantEntreprise;
    private LocalDateTime dateSignatureRepresentantEntreprise;
    private Long signataireRepresentantEntrepriseId;
    private String roleSignatureRepresentantEntreprise;
    private String nomSignataireRepresentantEntreprise;

    private Long stageId;
    private String stageTitre;
    private String stageSujet;
    private LocalDate stageDateDebut;
    private LocalDate stageDateFin;
    private String stagiaireNomComplet;
    private String sectionStagiaire;
    private String entrepriseNom;
    private String entrepriseLieuStage;

    private Long reunionFinaleId;
    private String reunionFinaleNumero;
    private LocalDate reunionFinaleDate;
    private LocalTime reunionFinaleHeure;

    private Double noteFinale;
    private Boolean signaturesCompletes;
    private Boolean complete;
    private Boolean verrouillee;
    private List<NoteAttribueeDto> notesAttribuees = new ArrayList<>();
}
