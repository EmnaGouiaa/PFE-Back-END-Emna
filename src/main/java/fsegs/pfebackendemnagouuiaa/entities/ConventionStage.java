package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Convention de stage.
 * <p>
 * Les anciens champs de signature (signeeEncPro, nomSignataireEncPro, etc.) ont été supprimés.
 * La liste {@link #signatures} remplace l'ensemble des booléens et champs role-spécifiques.
 * </p>
 */
@Entity
@Table(name = "conventions_stage")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConventionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer numConv;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    // ── Signatures ─────────────────────────────────────────────────────────────

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "convention_stage_id")
    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();

    // ── Méthodes métier ────────────────────────────────────────────────────────

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
        return signatures.stream().filter(s -> s.getRoleSignature() == role).findFirst();
    }

    /**
     * Retourne {@code true} si les 5 parties ont signé.
     */
    public boolean estCompletementSigne() {
        return estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE)
                && estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL)
                && estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE)
                && estSignePar(RoleSignature.RESPONSABLE_UNIVERSITAIRE)
                && estSignePar(RoleSignature.STAGIAIRE);
    }

    // ── Relations ──────────────────────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", unique = true)
    private Stage stage;

    @OneToMany(mappedBy = "conventionStage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "demande_stage_id")
    private DemandeCreationCompteEntreprise demandeStage;
}
