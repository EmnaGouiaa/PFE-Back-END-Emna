package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Filière d'études (ex. informatique, gestion) pour classer les {@link Stagiaire}.
 *
 * <h3>Mapping JPA</h3>
 * Table {@code filiere}, relation {@code @OneToMany} vers les stagiaires.
 *
 * <h3>Consommation applicative</h3>
 * {@code UtilisateurServiceImpl} ; contrôleur {@code FiliereController}.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Filiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String description;

    @OneToMany(mappedBy = "filiere")
    @JsonIgnore
    private List<Stagiaire> stagiaires = new ArrayList<>();
}