package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FicheEvaluation extends Formulaire{
    @Enumerated(EnumType.STRING)
    private TypeEvaluation type;

    private Double noteFinale;
    private String appreciation;
    private String pointFort;
    private String axeAmelioration;

    @ManyToOne
    @JoinColumn(name = "stage_id")
    private Stage stage;
    @ManyToOne
    @JoinColumn(name = "dossier_stage_id") // nom de la colonne FK
    private DossierStage dossierStage;
    @ManyToOne
    @JoinColumn(name = "evaluateur_id")
    private User evaluateur;

    @OneToOne(mappedBy = "ficheEvaluation")
    private ReunionFinale reunionFinale;

    // CLASSE ASSOCIATION : NoteAttribuee
    @OneToMany(mappedBy = "ficheEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteAttribuee> notesAttribuees = new ArrayList<>();

    @Override
    public String getType() { return "FICHE_EVALUATION"; }

    @Override
    public boolean estComplet() {
        return notesAttribuees.stream().allMatch(NoteAttribuee::estEvalue);
    }

    public Double calculerNoteFinale() {
        return notesAttribuees.stream()
                .mapToDouble(NoteAttribuee::calculerScorePondere)
                .sum();
    }

    public void ajouterCritere(CritereEvaluation critere, Integer poids) {
        NoteAttribuee na = new NoteAttribuee();
        na.setFicheEvaluation(this);
        na.setCritereEvaluation(critere);
        na.setPoids(poids);
        na.setBareme(critere.getBareme());
        this.notesAttribuees.add(na);
    }

}
