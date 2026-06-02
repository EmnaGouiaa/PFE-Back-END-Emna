package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Clé composite de {@link NoteAttribuee}.
 *
 * <p>Règle de nommage : les champs <strong>doivent</strong> correspondre aux attributs
 * référencés par {@code @MapsId} dans {@link NoteAttribuee} ({@code "ficheEvaluationId"}
 * et {@code "critereEvaluationId"}).</p>
 *
 * <p>Implements {@link Serializable} : requis par la spec JPA pour tout
 * {@code @Embeddable} utilisé comme clé primaire composée.</p>
 *
 * <h3>Correction K1</h3>
 * Suppression de l'annotation {@code @EqualsAndHashCode} redondante :
 * {@code @Data} (ici remplacé par les annotations granulaires) génère déjà
 * {@code equals} et {@code hashCode} — la double annotation était sans effet
 * mais créait une confusion de lecture.
 *
 * <h3>Consommation applicative</h3>
 * {@code NoteAttribueeServiceImpl}, {@code FicheEvaluationServiceImpl}.
 */
@Embeddable
@Getter                 // Pas de @Setter : la clé est immuable une fois assignée
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode      // Basé sur les deux champs Long — correct pour une clé composite
@ToString
public class CleNoteAttribuee implements Serializable {

    /**
     * Identifiant de la {@link FicheEvaluation}.
     * Mappé par {@code @MapsId("ficheEvaluationId")} dans {@link NoteAttribuee}.
     */
    private Long ficheEvaluationId;

    /**
     * Identifiant du {@link CritereEvaluation}.
     * Mappé par {@code @MapsId("critereEvaluationId")} dans {@link NoteAttribuee}.
     *
     * <p><strong>Ordre des arguments du constructeur :</strong>
     * {@code new CleNoteAttribuee(ficheId, critereId)} — fiche en premier,
     * critère en second. Respecter cet ordre dans tous les call sites.</p>
     */
    private Long critereEvaluationId;
}
