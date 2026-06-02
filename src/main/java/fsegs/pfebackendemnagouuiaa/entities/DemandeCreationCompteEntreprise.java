package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Demande de stage proposée par un stagiaire pour une entreprise non encore référencée
 * (création de compte entreprise + convention associée après validation).
 *
 * <h3>Mapping JPA</h3>
 * Table {@code demandes_stage}. Données entreprise et responsable dénormalisées sur la demande.
 * Double circuit de validation : {@link #statut} ({@link StatutDemande}) et
 * {@link #statutResponsableStages} ({@link StatutValidation}).
 * Lien 1-1 optionnel vers le {@link Stage} créé après acceptation.
 *
 * <h3>Consommation applicative</h3>
 * {@code DemandeCreationCompteEntrepriseServiceImpl} ;
 * contrôleur {@code DemandeCreationCompteEntrepriseController}.
 */
@Entity
@Table(name = "demandes_stage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCreationCompteEntreprise {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stagiaire_id", nullable = false)
    private Utilisateur stagiaire;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    private StatutValidation statutResponsableStages;

    @Column(length = 1000)
    private String commentaireResponsableStages;

    private String nomEntreprise;

    private String emailEntreprise;

    private String telephoneEntreprise;

    private String adresse;

    private String secteurActivite;

    private String nomResponsable;

    private String prenomResponsable;

    private String emailResponsable;

    private String telephoneResponsable;

    @Column(name = "cree_le", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime creeLe;

    @Column(name = "mis_a_jour_le")
    private LocalDateTime misAJourLe;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @OneToOne(mappedBy = "demandeStage")
    @JsonIgnore
    private Stage stage;

    /** Horodatage de création, date du jour et statuts initiaux {@code EN_ATTENTE}. */
    @PrePersist
    public void prePersist() {
        this.creeLe = LocalDateTime.now();
        this.dateDemande = LocalDate.now();

        if (this.statut == null) {
            this.statut = StatutDemande.EN_ATTENTE;
        }

        if (this.statutResponsableStages == null) {
            this.statutResponsableStages = StatutValidation.EN_ATTENTE;
        }
    }

    /** Met à jour {@link #misAJourLe} à chaque modification. */
    @PreUpdate
    public void preUpdate() {
        this.misAJourLe = LocalDateTime.now();
    }
}
