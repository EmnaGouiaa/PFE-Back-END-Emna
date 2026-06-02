package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Offre de stage publiée par une entreprise, soumise à validation universitaire avant affectation.
 *
 * <h3>Mapping JPA</h3>
 * Table {@code offre_stage}. Associations {@code @ManyToOne} vers {@link Entreprise},
 * {@link ResponsableEntreprise} (publication), {@link ResponsableServiceStages} (validation)
 * et {@link Stagiaire} (affectation). Relation inverse {@code @OneToMany} vers les {@link Stage}
 * créés à partir de cette offre.
 *
 * <h3>Champs clés</h3>
 * <ul>
 *   <li>{@link #statut} — workflow {@link StatutOffre} (attente, publiée, validée, affectée, etc.).</li>
 *   <li>{@link #motifRefus} — justification en cas de refus par le service des stages.</li>
 *   <li>{@link #encadrantPro} — encadrant professionnel désigné sur l'offre.</li>
 * </ul>
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Service : {@code OffreStageServiceImpl}.</li>
 *   <li>Contrôleur : {@code OffreStageController}.</li>
 * </ul>
 *
 * @see StatutOffre
 * @see Stage
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffreStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String descriptionMissions;

    private Integer duree;

    private String profilRecherche;

    private LocalDate dateDebutPrevue;

    private LocalDate datePublication;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private StatutOffre statut;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "entreprise_id")
    @JsonIgnore
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publiee_par_id")
    @JsonIgnore
    private ResponsableEntreprise publieePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validee_par_id")
    @JsonIgnore
    private ResponsableServiceStages valideePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_affecte_id")
    @JsonIgnore
    private Stagiaire stagiaireAffecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_pro_id")
    @JsonIgnoreProperties({"stages", "hibernateLazyInitializer", "handler"})
    private EncadrantProfessionnel encadrantPro;

    @OneToMany(mappedBy = "offreSource")
    @JsonIgnore
    private Set<Stage> stages = new HashSet<>();
}
