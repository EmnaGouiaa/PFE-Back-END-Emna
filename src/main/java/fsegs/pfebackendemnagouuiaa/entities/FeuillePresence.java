package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FeuillePresence extends Formulaire{
    private Integer semaine;
    private Double totalHeures;
    private Boolean signeeTuteur;
    private Boolean signeeStagiaire ;

    @ManyToOne
    @JoinColumn(name = "cahier_stage_id")
    private CahierStage cahierStage;

    @OneToMany(mappedBy = "feuillePresence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JourPresence> joursPresence = new ArrayList<>();

    @Override
    public String getType() { return "FEUILLE_PRESENCE"; }

    @Override
    public boolean estComplet() {
        return signeeStagiaire && signeeTuteur ;
    }

    public Double calculerTotalHeures() {
        return joursPresence.stream()
                .mapToDouble(JourPresence::calculerHeures)
                .sum();
    }
}
