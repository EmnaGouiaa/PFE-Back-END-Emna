package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Étudiant en stage : sous-type de {@link Utilisateur} ({@code @DiscriminatorValue("Stagiaire")}).
 *
 * <h3>Mapping JPA</h3>
 * Table fille liée par héritage JOINED. {@link #filiere} en {@code @ManyToOne},
 * {@link #stages} en {@code @OneToMany} vers les stages suivis.
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Services : {@code StagiaireServiceImpl}, {@code UtilisateurServiceImpl},
 *       {@code OffreStageServiceImpl}.</li>
 *   <li>Contrôleur : {@code StagiaireController}.</li>
 *   <li>Dépôt : {@code StagiaireRepository}.</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"filiere", "stages", "encadrantAcademique"})
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("Stagiaire")
//pour stocker dans la base de données le type réel de chaque entité héritée
public class Stagiaire extends Utilisateur {
    private LocalDate dateNaiss;

    private Integer niveau;

    @OneToMany(mappedBy = "stagiaire")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_academique_id")
    @JsonIgnore
    private EncadrantAcademique encadrantAcademique;

    @ManyToOne
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

}
