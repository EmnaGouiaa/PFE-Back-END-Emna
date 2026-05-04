package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteAttribuee {

    @EmbeddedId
    private CleNoteAttribuee id;

    @ManyToOne
    @JoinColumn(name = "fiche_evaluation_id")
    private FicheEvaluation ficheEvaluation;

    @ManyToOne
    @JoinColumn(name = "critere_evaluation_id")
    private CritereEvaluation critereEvaluation;

    private Integer poids;
    private Integer bareme;
    private Integer note;
    private String commentaire;

    public boolean estEvalue() {
        return note != null;
    }

    public Double calculerScorePondere() {
        if (note == null || poids == null || bareme == null || bareme <= 0) {
            return 0.0;
        }
        return (note.doubleValue() / bareme.doubleValue()) * poids.doubleValue();
    }
}