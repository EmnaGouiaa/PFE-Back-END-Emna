package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_reunion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"participants"})
public class Reunion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate date;

    private String objet;

    @Column(length = 2000)
    private String compteRendu;

    @Enumerated(EnumType.STRING)
    private StatutReunion statut;
    @ManyToOne
    @JoinColumn(name = "dossier_stage_id")
    private DossierStage dossierStage;

    @ManyToMany
    @JoinTable(
            name = "reunion_participants",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<User> participants = new HashSet<>();

    public String getType() {
        return null;
    }

    public void planifier() { this.statut = StatutReunion.PLANIFIEE; }
    public void terminer() { this.statut = StatutReunion.TERMINEE; }





}
