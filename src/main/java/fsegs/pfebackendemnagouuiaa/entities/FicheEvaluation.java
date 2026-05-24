package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fiche d'évaluation de stage.
 *
 * <h3>Cycle de vie</h3>
 * <ol>
 *   <li>Création par l'encadrant professionnel ou le représentant entreprise.</li>
 *   <li>Remplissage par l'encadrant professionnel (notes + points forts/axes EP).</li>
 *   <li>Remplissage par le représentant entreprise (points forts/axes RE).</li>
 *   <li>Signature des deux parties → fiche verrouillée.</li>
 * </ol>
 *
 * <h3>Règle de verrouillage</h3>
 * La fiche est verrouillée ({@link #estVerrouillee()}) dès que les deux signataires
 * ({@link RoleSignature#ENCADRANT_PROFESSIONNEL} et
 * {@link RoleSignature#RESPONSABLE_ENTREPRISE}) ont signé.
 * Aucune modification n'est possible après verrouillage.
 *
 * <h3>Note finale</h3>
 * {@link #noteFinale} est une valeur <em>persistée et mise en cache</em> calculée
 * par {@link #calculerNoteFinale()}. Le service est responsable d'appeler
 * {@code fiche.setNoteFinale(fiche.calculerNoteFinale())} et de persister après
 * chaque modification de notes. Ce champ ne doit jamais être calculé en dehors
 * d'un contexte transactionnel avec {@code notesAttribuees} chargées.
 *
 * <h3>Corrections appliquées</h3>
 * <ul>
 *   <li><b>F1 — @Data supprimé</b> : remplacé par {@code @Getter @Setter +
 *       @ToString(exclude = {...})}. {@code @Data} générait un {@code hashCode()}
 *       qui traversait {@code notesAttribuees → NoteAttribuee → ficheEvaluation →
 *       notesAttribuees} → boucle infinie (StackOverflowError).
 *       {@code equals}/{@code hashCode} sont maintenant basés sur {@code id} seul.</li>
 *   <li><b>F2 — {@code notifications} : cascade réduite à PERSIST+MERGE</b>.
 *       {@code CascadeType.ALL + orphanRemoval} aurait supprimé toutes les
 *       notifications lors de la suppression d'une fiche, effaçant l'historique de
 *       communication. Les notifications sont un registre d'audit géré par
 *       {@code NotificationService}, pas par la fiche elle-même.</li>
 *   <li><b>F3 — contrat de {@code calculerNoteFinale()} documenté</b> : la méthode
 *       suppose que {@code notesAttribuees} est chargée en mémoire. Si la collection
 *       est vide car non initialisée (lazy), elle retourne {@code 0.0} — ce qui est
 *       correct <em>dans une transaction</em> avec toutes les notes présentes en
 *       mémoire. Le service ne doit l'appeler qu'après avoir accédé aux notes.</li>
 *   <li><b>F4 — {@code estVerrouillee()} conservé</b> : délègue à
 *       {@code estCompletementSigne()} intentionnellement. Cette séparation permet
 *       de modifier la règle de verrouillage (ex. ajouter une signature académique)
 *       sans changer tous les appels à {@code estVerrouillee()}.</li>
 * </ul>
 */
@Entity
@Table(name = "fiche_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"signatures", "notesAttribuees", "notifications", "stage", "reunionFinale"})
public class FicheEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Partie Encadrant Professionnel ─────────────────────────────────────────

    /** Points forts constatés par l'encadrant professionnel. Obligatoire avant signature EP. */
    @Column(columnDefinition = "TEXT")
    private String pointFortEncadrantPro;

    /** Axes d'amélioration identifiés par l'encadrant professionnel. Obligatoire avant signature EP. */
    @Column(columnDefinition = "TEXT")
    private String axeAmeliorationEncadrantPro;

    // ── Partie Responsable Entreprise ──────────────────────────────────────────

    /** Points forts constatés par le représentant de l'entreprise. Obligatoire avant signature RE. */
    @Column(columnDefinition = "TEXT")
    private String pointFortResponsableEntreprise;

    /** Axes d'amélioration identifiés par le représentant de l'entreprise. Obligatoire avant signature RE. */
    @Column(columnDefinition = "TEXT")
    private String axeAmeliorationResponsableEntreprise;

    // ── Note finale ────────────────────────────────────────────────────────────

    /**
     * Note finale persistée (cache calculé).
     * Mise à jour par le service via {@link #calculerNoteFinale()} après chaque
     * modification de {@link NoteAttribuee}. Ne jamais lire directement sans
     * s'assurer que la valeur est à jour (appeler le service).
     */
    @Column(name = "note_finale")
    private Double noteFinale;

    // ── Signatures ─────────────────────────────────────────────────────────────

    /**
     * Signatures apposées sur cette fiche.
     * {@code CascadeType.ALL + orphanRemoval} : les signatures appartiennent
     * exclusivement à cette fiche et doivent être supprimées avec elle.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_evaluation_id")
    private List<Signature> signatures = new ArrayList<>();

    // ── Relations ──────────────────────────────────────────────────────────────

    /**
     * Stage évalué par cette fiche. Relation 1-1 : un stage ne peut avoir qu'une seule fiche.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", unique = true, nullable = false)
    private Stage stage;

    /**
     * Réunion finale au cours de laquelle cette fiche est présentée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reunion_finale_id", nullable = false)
    private ReunionFinale reunionFinale;

    /**
     * Notes attribuées par critère dans cette fiche.
     * {@code CascadeType.ALL + orphanRemoval} : supprimer la fiche supprime toutes ses notes.
     */
    @OneToMany(mappedBy = "ficheEvaluation",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<NoteAttribuee> notesAttribuees = new ArrayList<>();

    /**
     * Notifications générées en lien avec cette fiche (ex. "fiche disponible", "signature requise").
     *
     * <p><b>Correction F2</b> : cascade réduite à {@code PERSIST + MERGE} (suppression
     * de {@code REMOVE} et {@code orphanRemoval}). Les notifications constituent un
     * historique d'audit ; les supprimer lors de la suppression de la fiche effacerait
     * des données importantes. Le cycle de vie des notifications est géré par
     * {@code NotificationService}, pas par cette entité.</p>
     */
    @OneToMany(mappedBy = "ficheEvaluation",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    // ── Méthodes de lecture d'état ─────────────────────────────────────────────

    /**
     * Retourne {@code true} si ce document a été signé par la partie identifiée par {@code role}.
     */
    public boolean estSignePar(RoleSignature role) {
        return signatures.stream().anyMatch(s -> s.getRoleSignature() == role);
    }

    /**
     * Retourne la signature de la partie identifiée par {@code role}, si elle existe.
     */
    public Optional<Signature> getSignaturePour(RoleSignature role) {
        return signatures.stream()
                .filter(s -> s.getRoleSignature() == role)
                .findFirst();
    }

    /**
     * Retourne {@code true} si les deux parties obligatoires ont signé :
     * {@link RoleSignature#ENCADRANT_PROFESSIONNEL} ET {@link RoleSignature#RESPONSABLE_ENTREPRISE}.
     */
    public boolean estCompletementSigne() {
        return estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL)
                && estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE);
    }

    /**
     * Retourne {@code true} si la fiche est verrouillée et ne peut plus être modifiée.
     *
     * <p><b>Note F4</b> : délègue intentionnellement à {@link #estCompletementSigne()}.
     * Cette séparation sémantique permet de faire évoluer la règle de verrouillage
     * indépendamment de la règle de signature (ex. ajout d'une troisième signature
     * académique obligatoire).</p>
     */
    public boolean estVerrouillee() {
        return estCompletementSigne();
    }

    // ── Méthodes de calcul ─────────────────────────────────────────────────────

    /**
     * Calcule la note finale comme somme des scores pondérés :
     * <pre>noteFinale = Σ (note_i / bareme_i) × poids_i</pre>
     *
     * <p>Pour un résultat sur 20, la somme des {@code poids} de toutes les notes
     * doit être égale à 20. Cette invariant est imposé par le service, pas par
     * cette méthode.</p>
     *
     * <p><b>Pré-condition (F3)</b> : {@code notesAttribuees} doit être chargée
     * en mémoire avant l'appel. N'appeler cette méthode que dans un contexte
     * transactionnel où la collection est initialisée.</p>
     *
     * @return somme des scores pondérés, ou {@code 0.0} si aucune note n'est évaluée.
     */
    public double calculerNoteFinale() {
        if (notesAttribuees == null || notesAttribuees.isEmpty()) {
            return 0.0;
        }
        return notesAttribuees.stream()
                .filter(NoteAttribuee::estEvalue)
                .mapToDouble(NoteAttribuee::calculerScorePondere)
                .sum();
    }

    // ── Méthodes de validation métier ──────────────────────────────────────────

    /**
     * Retourne {@code true} si toutes les notes ont été saisies (aucune à null).
     */
    public boolean toutesLesNotesSontRenseignees() {
        return notesAttribuees != null
                && !notesAttribuees.isEmpty()
                && notesAttribuees.stream().allMatch(NoteAttribuee::estEvalue);
    }

    /**
     * Retourne {@code true} si la section de l'encadrant professionnel est complète
     * (points forts et axes d'amélioration non vides).
     */
    public boolean partieEncadrantProfessionnelComplete() {
        return isNotBlank(pointFortEncadrantPro)
                && isNotBlank(axeAmeliorationEncadrantPro);
    }

    /**
     * Retourne {@code true} si la section du représentant entreprise est complète.
     */
    public boolean partieResponsableEntrepriseComplete() {
        return isNotBlank(pointFortResponsableEntreprise)
                && isNotBlank(axeAmeliorationResponsableEntreprise);
    }

    /**
     * Retourne {@code true} si toutes les données requises sont renseignées :
     * sections EP et RE complètes, et toutes les notes saisies.
     * Précondition de la signature.
     */
    public boolean donneesCompletes() {
        return partieEncadrantProfessionnelComplete()
                && partieResponsableEntrepriseComplete()
                && toutesLesNotesSontRenseignees();
    }

    // ── equals / hashCode sur l'id uniquement (F1) ────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FicheEvaluation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

    // ── Utilitaire privé ───────────────────────────────────────────────────────

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
