package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DossierStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private String anneeUniversitaire;


    private LocalDate dateOuverture;

    private LocalDate dateCloture;

    @Enumerated(EnumType.STRING)
    private StatutDossier statut;



    @ManyToOne
    @JoinColumn(name = "stagiaire_id")
    private Etudiant stagiaire;
    @OneToOne(mappedBy = "dossierStage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Stage stage;

    @OneToOne(mappedBy = "dossierStage", cascade = CascadeType.ALL)
    @JsonIgnore
    private ConventionStage conventionStage ;

    @OneToOne(mappedBy = "dossierStage", cascade = CascadeType.ALL)
    @JsonIgnore
    private CahierStage cahierStage;
    @OneToMany(mappedBy = "dossierStage", cascade = CascadeType.ALL)
    private List<FicheEvaluation> fichesEvaluation = new ArrayList<>();

    @OneToMany(mappedBy = "dossierStage", cascade = CascadeType.ALL)
    private List<EnqueteSatisfaction> enquetes = new ArrayList<>();

    @OneToMany(mappedBy = "dossierStage", cascade = CascadeType.ALL)
    private List<Reunion> reunions = new ArrayList<>();


    public ConventionStage creerConvention() {
        ConventionStage conv = new ConventionStage();
        conv.setDossierStage(this);
        conv.setStage(this.stage);
        conv.setStatut(StatutConvention.EN_COURS);
        this.conventionStage = conv;
        return conv;
    }

    public CahierStage ouvrirCahierStage() {
        CahierStage cs = new CahierStage();
        cs.setDossierStage(this);
        cs.initialiserFeuilles(this.stage.getDureeSemaines());
        this.cahierStage = cs;
        return cs;
    }

    public ReunionHebdomadaire planifierReunionHebdomadaire() {
        ReunionHebdomadaire rh = new ReunionHebdomadaire();
        rh.setCahierStage(this.cahierStage);
        rh.planifier();
        this.reunions.add(rh);
        return rh;
    }

    public ReunionFinale planifierSoutenance(FicheEvaluation ficheEval) {
        ReunionFinale rf = new ReunionFinale();
        rf.setStage(this.stage);
        rf.setFicheEvaluation(ficheEval);
        rf.planifier();
        this.reunions.add(rf);
        return rf;
    }

    public EnqueteSatisfaction lancerEnquete(CibleEnquete cible) {
        EnqueteSatisfaction es = new EnqueteSatisfaction();
        es.setCible(cible);
        es.setStage(this.stage);
        es.setDateCreation(LocalDate.now());
        es.setCompletee(false);
        this.enquetes.add(es);
        return es;
    }

    public boolean verifierCompletude() {
        return conventionStage != null && conventionStage.estSigneeParTous()
                && cahierStage != null
                && fichesEvaluation.size() == 2
                && enquetes.size() == 2;
    }


    private String calculerMention() {
        return "BIEN";
    }

    public void cloturer() {
        this.statut = StatutDossier.CLOTURE;
        this.dateCloture = LocalDate.now();

    }


}
