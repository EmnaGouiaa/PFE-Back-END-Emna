package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Période de stage académique définie dans le système.
 *
 * Une période active couvre la fenêtre pendant laquelle un stagiaire peut être
 * affecté à une offre de stage.  Plusieurs périodes peuvent coexister (ex. :
 * Période 1 académique — fév. à mai, Période 2 estivale — juin à août).
 *
 * La règle métier : l'affectation d'un stagiaire est bloquée si aucune période
 * active ne couvre la date du jour.
 *
 * <h3>Consommation applicative</h3>
 * {@code PeriodeStageServiceImpl} ; dépôt {@code PeriodeStageRepository}.
 */
@Entity
@Table(name = "periode_stage")
@Getter
@Setter
@NoArgsConstructor
public class PeriodeStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Libellé lisible, ex. "Période 1 — Stage académique 2025". */
    @Column(nullable = false, length = 200)
    private String libelle;

    /** Premier jour de la période (inclus). */
    @Column(nullable = false)
    private LocalDate dateDebut;

    /** Dernier jour de la période (inclus). */
    @Column(nullable = false)
    private LocalDate dateFin;

    /**
     * Indique si la période est active.
     * Une période inactive n'ouvre pas l'affectation même si la date du jour
     * est comprise dans sa fenêtre.
     */
    @Column(nullable = false)
    private boolean active = true;

    public PeriodeStage(String libelle, LocalDate dateDebut, LocalDate dateFin, boolean active) {
        this.libelle   = libelle;
        this.dateDebut = dateDebut;
        this.dateFin   = dateFin;
        this.active    = active;
    }
}
