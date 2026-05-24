package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cahier de stage.
 * <p>
 * Les anciens champs de signature role-spécifiques ont été supprimés.
 * La liste {@link #signatures} porte l'intégralité des informations de signature.
 * {@code dateSignature} (LocalDate) est conservé : il représente la date inscrite
 * dans le document lors de la dernière signature.
 * </p>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateGeneration;

    /** Date de signature inscrite dans le document (mise à jour à chaque signature). */
    private LocalDate dateSignature;

    // ── Signatures ─────────────────────────────────────────────────────────────

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cahier_stage_id")
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
     * Retourne {@code true} si les 4 parties ont signé.
     */
    public boolean estCompletementSigne() {
        return estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE)
                && estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL)
                && estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE)
                && estSignePar(RoleSignature.STAGIAIRE);
    }

    // ── Relations ──────────────────────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", unique = true)
    private Stage stage;

    @OneToMany(mappedBy = "cahierStage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reunion> reunions = new ArrayList<>();

    @OneToMany(mappedBy = "cahierStage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();
}
