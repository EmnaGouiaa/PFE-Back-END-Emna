package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Représentant légal ou administratif de l'entreprise : publie les offres, signe les documents.
 *
 * <h3>Mapping JPA</h3>
 * Sous-type JOINED de {@link Utilisateur}, lié à {@link Entreprise} ({@code @ManyToOne}).
 * Peut être désigné comme {@link Stage#tuteurEntreprise}.
 *
 * <h3>Consommation applicative</h3>
 * Services métier entreprise (offres, conventions, fiches) ;
 * contrôleurs {@code ResponsableEntrepriseController}, {@code AdminRepresentantEntrepriseController}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"entreprise"})
@SuperBuilder
@DiscriminatorValue("ResponsableEntreprise")
public class ResponsableEntreprise extends Utilisateur {

    private String poste;

    private String service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"responsables"})
    private Entreprise entreprise;

}
