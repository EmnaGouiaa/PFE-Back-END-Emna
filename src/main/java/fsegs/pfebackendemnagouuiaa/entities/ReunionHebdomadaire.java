package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("HEBDOMADAIRE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReunionHebdomadaire extends Reunion{


    private Integer numSemaine;
    private String objectifsSemaineProchaine;

    @ManyToOne
    @JoinColumn(name = "cahier_stage_id")
    private CahierStage cahierStage;
}
