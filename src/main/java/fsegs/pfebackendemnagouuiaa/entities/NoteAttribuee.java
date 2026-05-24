package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Note attribuée pour un critère donné dans une fiche d'évaluation.
 *
 * <h3>Formule de calcul</h3>
 * {@code scorePondere = (note / bareme) * poids}
 * <p>La somme de tous les {@code poids} doit être égale à 20 pour que
 * {@code noteFinale} soit exprimée sur 20. Cette contrainte est imposée
 * par le service, pas par l'entité.</p>
 *
 * <h3>Mapping JPA</h3>
 * {@code @MapsId} relie chaque champ de la clé composite ({@code @EmbeddedId})
 * à la colonne physique portée par le {@code @ManyToOne} correspondant.
 * Cela évite la duplication de colonnes (sans @MapsId, Hibernate créerait
 * 4 colonnes au lieu de 2).
 *
 * <h3>Corrections appliquées</h3>
 * <ul>
 *   <li><b>N1 — @Data supprimé</b> : remplacé par {@code @Getter @Setter}.
 *       {@code @Data} générait un {@code hashCode()} incluant
 *       {@code ficheEvaluation} et {@code critereEvaluation}, créant une
 *       boucle infinie avec {@code FicheEvaluation.notesAttribuees}.
 *       {@code equals}/{@code hashCode} sont maintenant basés sur la
 *       clé composite {@code id} uniquement.</li>
 *   <li><b>N2 — @Column(nullable = false)</b> ajouté sur {@code poids},
 *       {@code bareme} et {@code note} (les deux premiers sont toujours requis ;
 *       {@code note} peut être null avant évaluation et reste nullable).</li>
 *   <li><b>N3 — {@code calculerScorePondere()}</b> : le cas {@code bareme == null}
 *       est maintenant journalisé via {@code System.err} pour ne pas masquer
 *       un problème d'initialisation.</li>
 * </ul>
 */
@Entity
@Table(name = "note_attribuee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"ficheEvaluation", "critereEvaluation"})
// ⚠️ equals/hashCode basés sur la clé composite id, pas sur les relations
public class NoteAttribuee {

    @EmbeddedId
    private CleNoteAttribuee id;

    /**
     * Fiche d'évaluation à laquelle cette note appartient.
     * {@code @MapsId} lie le champ {@code ficheEvaluationId} de la clé composite
     * à cette colonne FK — une seule colonne physique {@code fiche_evaluation_id}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ficheEvaluationId")
    @JoinColumn(name = "fiche_evaluation_id", nullable = false)
    private FicheEvaluation ficheEvaluation;

    /**
     * Critère d'évaluation pour lequel cette note est attribuée.
     * {@code @MapsId} lie le champ {@code critereEvaluationId} de la clé composite
     * à cette colonne FK.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("critereEvaluationId")
    @JoinColumn(name = "critere_evaluation_id", nullable = false)
    private CritereEvaluation critereEvaluation;

    /**
     * Coefficient de pondération de ce critère dans le calcul de la note finale.
     * La somme de tous les poids dans une fiche doit valoir 20.
     * Non null : le service garantit l'initialisation avant persistance.
     */
    @Column(nullable = false)
    private Integer poids;

    /**
     * Note maximale attribuable pour ce critère.
     * Initialisé à partir de {@link CritereEvaluation#getBareme()} lors de la création.
     * Conservé ici pour une éventuelle modification future du barème du critère sans
     * invalider les notes historiques (dénormalisation intentionnelle).
     * Non null : le service garantit l'initialisation avant persistance.
     */
    @Column(nullable = false)
    private Integer bareme;

    /**
     * Note effectivement attribuée (entre 0 et {@code bareme} inclus).
     * Null si le critère n'a pas encore été évalué ({@link #estEvalue()} retourne false).
     */
    @Column
    private Integer note;

    /**
     * Commentaire libre de l'évaluateur pour justifier la note.
     * Distinct de {@link CritereEvaluation#getConsigne()} qui est une consigne générale.
     */
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ── Méthodes métier ──────────────────────────────────────────────────────────

    /**
     * Retourne {@code true} si une note a été saisie pour ce critère.
     */
    public boolean estEvalue() {
        return note != null;
    }

    /**
     * Calcule la contribution pondérée de cette note : {@code (note / bareme) * poids}.
     *
     * <p>Retourne {@code 0.0} si l'une des valeurs requises est absente.
     * Si {@code bareme} est null ou ≤ 0, un avertissement est écrit sur stderr
     * car cela indique un problème d'initialisation (N3).</p>
     */
    public double calculerScorePondere() {
        if (note == null || poids == null) {
            return 0.0;
        }
        if (bareme == null || bareme <= 0) {
            System.err.printf(
                "[NoteAttribuee] bareme invalide (id=%s) — scorePondere forcé à 0%n", id);
            return 0.0;
        }
        return (note.doubleValue() / bareme.doubleValue()) * poids.doubleValue();
    }

    // ── equals / hashCode sur la clé composite ────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoteAttribuee other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
