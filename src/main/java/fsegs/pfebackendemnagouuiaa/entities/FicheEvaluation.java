package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FicheEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Partie Encadrant Professionnel
    private String pointFortEncadrantPro;
    private String axeAmeliorationEncadrantPro;
    private String signatureEncadrantProfessionnel;
    private LocalDateTime dateSignatureEncadrantProfessionnel;
    private Long signataireEncadrantProfessionnelId;
    private String roleSignatureEncadrantProfessionnel;
    private String nomSignataireEncadrantProfessionnel;
    private Double noteFinale;
    // Partie Responsable Entreprise
    private String pointFortResponsableEntreprise;
    private String axeAmeliorationResponsableEntreprise;
    private String signatureRepresentantEntreprise;
    private LocalDateTime dateSignatureRepresentantEntreprise;
    private Long signataireRepresentantEntrepriseId;
    private String roleSignatureRepresentantEntreprise;
    private String nomSignataireRepresentantEntreprise;

    @OneToOne
    @JoinColumn(name = "stage_id", unique = true, nullable = false)
    private Stage stage;

    @ManyToOne
    @JoinColumn(name = "reunion_finale_id", nullable = false)
    private ReunionFinale reunionFinale;

    @OneToMany(mappedBy = "ficheEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteAttribuee> notesAttribuees = new ArrayList<>();

    public Double calculerNoteFinale() {
        if (notesAttribuees == null || notesAttribuees.isEmpty()) {
            return 0.0;
        }

        return notesAttribuees.stream()
                .filter(NoteAttribuee::estEvalue)
                .mapToDouble(NoteAttribuee::calculerScorePondere)
                .sum();
    }

    public boolean toutesLesNotesSontRenseignees() {
        return notesAttribuees != null
                && !notesAttribuees.isEmpty()
                && notesAttribuees.stream().allMatch(NoteAttribuee::estEvalue);
    }


    public boolean partieEncadrantProfessionnelComplete() {
        return pointFortEncadrantPro != null && !pointFortEncadrantPro.isBlank()
                && axeAmeliorationEncadrantPro != null && !axeAmeliorationEncadrantPro.isBlank();
    }
    public boolean partieResponsableEntrepriseComplete() {
        return pointFortResponsableEntreprise != null && !pointFortResponsableEntreprise.isBlank()
                && axeAmeliorationResponsableEntreprise != null && !axeAmeliorationResponsableEntreprise.isBlank();
    }
    public boolean donneesCompletes() {
        return partieEncadrantProfessionnelComplete()
                && partieResponsableEntrepriseComplete()
                && toutesLesNotesSontRenseignees();
    }
    public boolean signaturesCompletes() {
        return signatureEncadrantProfessionnel != null && !signatureEncadrantProfessionnel.isBlank()
                && signatureRepresentantEntreprise != null && !signatureRepresentantEntreprise.isBlank();
    }
    public boolean estVerrouillee() {
        return signaturesCompletes();
    }
}
