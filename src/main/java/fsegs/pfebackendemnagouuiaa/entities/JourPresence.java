package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.time.LocalDate;
import java.util.Date;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JourPresence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate date;

    private Time heureArrivee;
    private Time heureDepart;
    private Boolean present;
    private String motifAbsence;

    @ManyToOne
    @JoinColumn(name = "feuille_presence_id")
    private FeuillePresence feuillePresence;

    public Double calculerHeures() {
        if (!present || heureArrivee == null || heureDepart == null) {
            return 0.0;
        }
        long diffMillis = heureDepart.getTime() - heureArrivee.getTime();
        return diffMillis / (1000.0 * 60 * 60);
    }
}
