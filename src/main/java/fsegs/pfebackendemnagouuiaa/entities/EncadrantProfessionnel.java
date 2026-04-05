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
public class EncadrantProfessionnel extends User {

    private String poste;

    private String service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "entreprise")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "encadrantProfessionnel")
    private List<Stage> stagesEncadres = new ArrayList<>();

    @OneToMany(mappedBy = "evaluateur")
    private List<FicheEvaluation> fichesEvaluees = new ArrayList<>();

    public void suivreStagiaire(Stage stage) {
        // Suivi technique quotidien
    }



    // AJOUTÉ : Remplit la fiche d'évaluation (mi-stage ou fin de stage)
    public FicheEvaluation remplirFicheEvaluation(Stage stage, TypeEvaluation type) {
        FicheEvaluation fe = new FicheEvaluation();
        fe.setStage(stage);
        fe.setEvaluateur(this);
        fe.setType(type);
        this.fichesEvaluees.add(fe);
        return fe;
    }

    public void validerRapportHebdomadaire(RapportHebdomadaire rapport) {
        rapport.setValideParEncadrant(true);
    }
}
