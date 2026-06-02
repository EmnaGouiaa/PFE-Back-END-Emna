package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Réunion liée à un stage (suivi hebdomadaire ou soutenance finale).
 *
 * <h3>Mapping JPA</h3>
 * Héritage {@link InheritanceType#SINGLE_TABLE} avec discriminant {@code type_reunion}.
 * Sous-types : {@link ReunionHebdomadaire} ({@code HEBDOMADAIRE}), {@link ReunionFinale} ({@code FINALE}).
 * Participants en {@code @ManyToMany} via {@code reunion_participants}.
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Services : {@code ReunionServiceImpl}, {@code ReunionFinaleServiceImpl}.</li>
 *   <li>Contrôleurs : {@code ReunionController}, {@code ReunionHebdomadaireController},
 *       {@code ReunionFinaleController}.</li>
 * </ul>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_reunion")
@Getter
@Setter

@ToString(exclude = {"participants", "stage", "cahierStage"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Reunion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numReunion;

    private LocalDate date;

    private LocalTime heure;

    private String observation;

    /** Observation de l'encadrant académique créateur (réunions hebdomadaires qu'il a planifiées). */
    @Column(length = 5000)
    private String observationEncadrantAcademique;

    /** Observation de l'encadrant professionnel créateur (réunions hebdomadaires qu'il a planifiées). */
    @Column(length = 5000)
    private String observationEncadrantProfessionnel;

    @Column(length = 5000)
    private String compteRendu;

    private Long encadrantCreateurId;

    private String typeEncadrantCreateur;

    private String nomEncadrantCreateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cahier_stage_id")
    private CahierStage cahierStage;

    @ManyToMany
    @JoinTable(
            name = "reunion_participants",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<Utilisateur> participants = new HashSet<>();

    @OneToMany(mappedBy = "reunion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();



}
