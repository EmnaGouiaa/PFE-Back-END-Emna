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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class EncadrantAcademique extends User {

    private String grade;

    private String specialite;

    private String departement;
    @OneToMany(mappedBy = "encadrantAcademique")
    private List<Stage> stagesEncadres = new ArrayList<>();

    @OneToMany(mappedBy = "evaluateur")
    private List<FicheEvaluation> fichesEvaluees = new ArrayList<>();


    public void evaluerRapport(RapportHebdomadaire rapport) {
        rapport.setCommentaireEncadrant("Évalué");
        rapport.valider();
    }


}
