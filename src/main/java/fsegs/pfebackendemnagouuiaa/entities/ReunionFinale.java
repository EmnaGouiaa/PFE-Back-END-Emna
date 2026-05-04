package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("Finale")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"ficheEvaluation"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReunionFinale extends Reunion {

    private Integer note;

    private String urlFormEvaluation;

    private String urlFormSatisfaction;

    private String titreEnqueteSatisfaction;

    @Column(columnDefinition = "TEXT")
    private String descriptionEnqueteSatisfaction;

    @OneToMany(mappedBy = "reunionFinale", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<FicheEvaluation> ficheEvaluation = new ArrayList<>();
}
