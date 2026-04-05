package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("Finale")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReunionFinale extends Reunion{
    private Double note;

    @OneToOne
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @OneToOne
    @JoinColumn(name = "fiche_evaluation_id")
    private FicheEvaluation ficheEvaluation;

    @Override
    public String getType() { return "FINALE"; }


    public void validerFicheEvaluation() {
        if (ficheEvaluation != null) {
            ficheEvaluation.valider();
        }
    }
}
