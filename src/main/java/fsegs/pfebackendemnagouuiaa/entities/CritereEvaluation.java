package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Critère d'évaluation rattaché à une {@link FicheEvaluation}.
 *
 * <h3>Sémantique des champs textuels</h3>
 * <ul>
 *   <li>{@link #libelle}     — Intitulé court affiché dans l'interface (obligatoire).</li>
 *   <li>{@link #description} — Explication détaillée du critère (optionnelle).
 *       Si absente, l'interface peut afficher {@code libelle} à sa place.</li>
 *   <li>{@link #categorie}   — Regroupement fonctionnel (ex. "Comportement", "Technique").
 *       Actuellement toujours "Evaluation du stage" — conservé pour extension future.</li>
 *   <li>{@link #consigne}    — Indication générale à l'évaluateur sur comment noter.
 *       Ne doit PAS contenir un commentaire de note — celui-ci est dans
 *       {@link NoteAttribuee#getCommentaire()}.</li>
 * </ul>
 *
 * <h3>Corrections appliquées</h3>
 * <ul>
 *   <li><b>C1 — @Data supprimé</b> : remplacé par {@code @Getter @Setter}.
 *       {@code @Data} générait un {@code hashCode()} incluant {@code fiche} et
 *       {@code notes}, traversant l'ensemble du graphe JPA → {@code StackOverflowError}.</li>
 *   <li><b>C2 — cascade + orphanRemoval sur {@code notes}</b> : sans cela, supprimer un
 *       critère échouait avec une violation de clé étrangère car les {@link NoteAttribuee}
 *       liées n'étaient pas supprimées automatiquement.</li>
 *   <li><b>C3 — {@code @Column(nullable = false)}</b> sur {@code libelle} et
 *       {@code partie} qui sont obligatoires selon les règles métier.</li>
 * </ul>
 */
@Entity
@Table(name = "critere_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"notes", "fiche"})
public class CritereEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Intitulé court du critère, affiché dans l'interface.
     * Obligatoire (C3).
     */
    @Column(nullable = false)
    private String libelle;

    /**
     * Description détaillée du critère. Optionnelle.
     * Si renseignée, elle complète le {@link #libelle} sans le remplacer.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Catégorie fonctionnelle du critère (ex. "Technique", "Comportement").
     * Actuellement toujours initialisé à "Evaluation du stage" par le service.
     * Conservé pour permettre un regroupement future des critères par catégorie.
     */
    private String categorie;

    /**
     * Note maximale attribuable pour ce critère.
     * Dans ce système, toujours 5. Copiée dans {@link NoteAttribuee#getBareme()}
     * lors de la création de la note (dénormalisation pour historique).
     */
    @Column(nullable = false)
    private Integer bareme = 5;

    /**
     * Consigne générale destinée à l'évaluateur.
     * Exemple : "Évaluer la rigueur méthodologique et la capacité d'adaptation."
     * Ne pas y stocker un commentaire spécifique à une note — utiliser
     * {@link NoteAttribuee#getCommentaire()} pour cela.
     */
    @Column(columnDefinition = "TEXT")
    private String consigne;

    /**
     * Partie de la fiche dans laquelle ce critère est évalué.
     * Obligatoire (C3).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartieEvaluation partie;

    /**
     * Notes attribuées pour ce critère, toutes fiches confondues.
     *
     * <p><b>Correction C2</b> : {@code cascade = ALL, orphanRemoval = true} ajoutés.
     * Sans eux, supprimer un critère déclenchait une violation de FK car les
     * {@link NoteAttribuee} liées n'étaient pas nettoyées automatiquement.</p>
     */
    @OneToMany(mappedBy = "critereEvaluation",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @JsonIgnore
    private List<NoteAttribuee> notes = new ArrayList<>();

    /**
     * Fiche d'évaluation à laquelle ce critère est rattaché.
     * {@code null} pour les critères globaux réutilisables (non encore utilisés
     * dans ce système — conservé pour extension future).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private FicheEvaluation fiche;

    // ── equals / hashCode sur l'id uniquement ────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CritereEvaluation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
