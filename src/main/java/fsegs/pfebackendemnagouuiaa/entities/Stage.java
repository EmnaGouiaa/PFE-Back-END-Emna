package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entité centrale du domaine « stage » : elle regroupe le sujet, le calendrier, les acteurs
 * (stagiaire, encadrants, entreprise) et les documents associés (convention, cahier, fiche d'évaluation).
 *
 * <h3>Rôle métier</h3>
 * Un {@code Stage} naît après affectation d'un stagiaire à une {@link OffreStage} ou à la suite
 * d'une {@link DemandeCreationCompteEntreprise} validée. Il pilote le cycle de vie académique et
 * professionnel jusqu'à la clôture ({@link StatutStage#TERMINE}).
 *
 * <h3>Mapping JPA</h3>
 * Table {@code stage} (nom par défaut Hibernate). Stratégie d'héritage absente : entité racine.
 * Chargement paresseux ({@code LAZY}) sur la plupart des associations pour limiter le graphe
 * lors des listes. Cascades {@code ALL} + {@code orphanRemoval} sur les documents 1-1
 * (convention, cahier, fiche) afin qu'ils suivent le cycle de vie du stage.
 *
 * <h3>Champs et relations clés</h3>
 * <ul>
 *   <li>{@link #statut} — {@link StatutStage}, transitions gérées par {@code StageServiceImpl}.</li>
 *   <li>{@link #statutSujet} — {@link StatutValidation} du sujet par l'encadrant académique.</li>
 *   <li>{@link #stagiaire}, {@link #encadrantAcademique}, {@link #encadrantProfessionnel},
 *       {@link #tuteurEntreprise} — acteurs du stage ({@code @ManyToOne}).</li>
 *   <li>{@link #entreprise} — hôte obligatoire du stage.</li>
 *   <li>{@link #offreSource} — offre d'origine (optionnelle si création via demande).</li>
 *   <li>{@link #conventionDeStage}, {@link #cahierStage}, {@link #ficheEvaluation} — documents 1-1.</li>
 *   <li>{@link #reunions}, {@link #absences}, {@link #notifications} — collections 1-N.</li>
 *   <li>Identifiants Trello — intégration tableau Kanban (optionnelle).</li>
 * </ul>
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Services : {@code StageServiceImpl}, {@code StageDocumentServiceImpl},
 *       {@code OffreStageServiceImpl}, {@code CompanyValidationServiceImpl},
 *       {@code StagiaireServiceImpl}, {@code EnqueteSatisfactionServiceImpl}.</li>
 *   <li>Contrôleurs : {@code StageController}, {@code StageDocumentController},
 *       {@code CompanyValidationController}.</li>
 *   <li>Dépôt : {@code StageRepository}.</li>
 * </ul>
 *
 * @see StatutStage
 * @see ConventionStage
 * @see CahierStage
 * @see FicheEvaluation
 */
@Entity
@Data
@EqualsAndHashCode(exclude = {
        "stagiaire",
        "encadrantAcademique",
        "encadrantProfessionnel",
        "tuteurEntreprise",
        "entreprise",
        "offreSource",
        "demandeStage",
        "conventionDeStage",
        "cahierStage",
        "ficheEvaluation",
        "fichesEvaluation",
        "reunions",
        "absences",
        "notifications"
})
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
        "cahierStage",
        "ficheEvaluation",
        "fichesEvaluation",
        "reunions",
        "notifications",
        "absences"
})
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String titre;

    private LocalDate dateDebut;

    private Integer duree;

    private LocalDate dateFin;

    private Integer nbSemaine;

    @Column(length = 500)
    private String niveauSouhaite;

    @Enumerated(EnumType.STRING)
    private StatutStage statut;

    @Column(columnDefinition = "TEXT")
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

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ConventionStage conventionDeStage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_stage_id")
    private DemandeCreationCompteEntreprise demandeStage;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private CahierStage cahierStage;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private FicheEvaluation ficheEvaluation;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reunion> reunions = new ArrayList<>();

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Absence> absences = new HashSet<>();

    private Boolean sectionEvaluationOuverte = Boolean.FALSE;

    private Boolean notificationOuvertureEspacesEnvoyee = Boolean.FALSE;

    /** Statut métier pour le suivi (En cours / Terminé / Refusé). */
    @JsonProperty("statutSuivi")
    public String getStatutSuivi() {
        return StatutSuiviStage.from(statut).name();
    }

    @JsonProperty("statutSuiviLibelle")
    public String getStatutSuiviLibelle() {
        return StatutSuiviStage.from(statut).getLibelle();
    }
}
