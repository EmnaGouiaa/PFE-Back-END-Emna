package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Enseignant universitaire encadrant des stagiaires et validant les sujets de stage.
 *
 * <h3>Mapping JPA</h3>
 * Sous-type JOINED de {@link Utilisateur}. {@code @OneToMany} vers {@link Stage} et {@link Stagiaire}.
 *
 * <h3>Consommation applicative</h3>
 * {@code UtilisateurServiceImpl}, {@code StageServiceImpl} ;
 * contrôleur {@code EncadrantAcademiqueController}.
 */
@Entity
@DiscriminatorValue("EncadrantAcademique") // si SINGLE_TABLE
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

public class EncadrantAcademique extends Utilisateur {

    private String grade;

    private String specialite;

    @OneToMany(mappedBy = "encadrantAcademique")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "encadrantAcademique")
    @JsonIgnore
    private List<Stagiaire> stagiaires = new ArrayList<>();
}
