package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent du service des stages universitaire : valide les offres et supervise le parcours global.
 *
 * <h3>Mapping JPA</h3>
 * Sous-type JOINED de {@link Utilisateur}. Relation inverse vers les offres validées
 * ({@link OffreStage#valideePar}).
 *
 * <h3>Consommation applicative</h3>
 * {@code ResponsableServiceStagesServiceImpl}, {@code OffreStageServiceImpl} ;
 * contrôleur {@code ResponsableServiceStagesController}.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResponsableServiceStages extends Utilisateur {

    private String service;

    @OneToMany(mappedBy = "valideePar")
    @JsonIgnore
    private List<OffreStage> offresValidees = new ArrayList<>();
}