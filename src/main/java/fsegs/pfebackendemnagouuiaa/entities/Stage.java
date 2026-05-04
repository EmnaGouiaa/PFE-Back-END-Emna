package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "stagiaire",
        "encadrantAcademique",
        "tuteurEntreprise",
        "encadrantProfessionnel",
        "entreprise",
        "offreSource",
        "demandeStage",
        "conventionDeStage",
        "fichesEvaluation",
        "reunions",
        "absences"
})
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    private LocalDate dateDebut;

    private Integer duree;

    private LocalDate dateFin;

    private Integer nbSemaine;

    private String niveauSouhaite;

    @Enumerated(EnumType.STRING)
    private StatutStage statut;
    private String sujet;
    private String trelloBoardId;
    private String trelloBoardUrl;
    private String trelloTodoListId;
    private String trelloDoingListId;
    private String trelloDoneListId;

    @Enumerated(EnumType.STRING)
    private StatutValidation statutSujet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sujet_valide_par_id")
    private EncadrantAcademique sujetValidePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonManagedReference
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_academique_id")
    private EncadrantAcademique encadrantAcademique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuteur_entreprise_id")
    private ResponsableEntreprise tuteurEntreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_professionnel_id")
    private EncadrantProfessionnel encadrantProfessionnel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_source_id")
    private OffreStage offreSource;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private ConventionStage conventionDeStage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_stage_id")
    private DemandeCreationCompteEntreprise demandeStage;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private FicheEvaluation ficheEvaluation;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Reunion> reunions;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Absence> absences = new HashSet<>();
}
