package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_reunion")
@Getter
@Setter

@ToString(exclude = {"participants", "stage"})
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

    @Column(columnDefinition = "TEXT")
    private String compteRendu;

    private Long encadrantCreateurId;

    private String typeEncadrantCreateur;

    private String nomEncadrantCreateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @ManyToMany
    @JoinTable(
            name = "reunion_participants",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<Utilisateur> participants = new HashSet<>();



}
