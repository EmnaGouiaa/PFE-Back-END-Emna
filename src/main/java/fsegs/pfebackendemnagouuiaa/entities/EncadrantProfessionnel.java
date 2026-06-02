package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Encadrant côté entreprise : suit le stagiaire sur le terrain et participe aux évaluations.
 *
 * <h3>Mapping JPA</h3>
 * Sous-type JOINED de {@link Utilisateur}, rattaché à une {@link Entreprise} ({@code @ManyToOne}).
 *
 * <h3>Consommation applicative</h3>
 * {@code OffreStageServiceImpl}, {@code FicheEvaluationServiceImpl} ;
 * contrôleur {@code EncadrantProfessionnelController}.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"entreprise", "stages"})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@DiscriminatorValue("EncadrantProfessionnel")
public class EncadrantProfessionnel extends Utilisateur {

    private String poste;

    private String service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"tuteurs"})
    private Entreprise entreprise;

    @OneToMany(mappedBy = "encadrantProfessionnel")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    /**
     * Valeurs par défaut à la création : rôle {@link Role#ENCADRANT_PROFESSIONNEL}
     * et compte actif si non renseignés.
     */
    @PrePersist
    private void applyDefaults() {
        if (getRole() == null) {
            setRole(Role.ENCADRANT_PROFESSIONNEL);
        }
        if (getActif() == null) {
            setActif(true);
        }
    }

}
