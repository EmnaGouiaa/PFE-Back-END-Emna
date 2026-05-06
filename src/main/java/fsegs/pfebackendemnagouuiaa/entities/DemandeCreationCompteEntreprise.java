package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private StatutValidation statutAdmin;

    @Enumerated(EnumType.STRING)
    private StatutValidation statutResponsableStages;

    @Column(length = 1000)
    private String commentaireAdmin;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "validee_par_encadrant_academique_id")
    private Utilisateur valideeParEncadrantAcademique;

    @Column(name = "validee_par_admin_id")
    private Long valideeParAdminId;

    @Column(name = "validee_par_encadrant_id")
    private Long valideeParEncadrantId;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "mis_a_jour_le")
    private LocalDateTime misAJourLe;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @OneToOne(mappedBy = "demandeStage")
    @JsonIgnore
    private Stage stage;
    @PrePersist
    public void prePersist() {
        this.creeLe = LocalDateTime.now();
        this.dateDemande = LocalDate.now();

        if (this.statut == null) {
            this.statut = StatutDemande.EN_ATTENTE;
        }

        if (this.statutAdmin == null) {
            this.statutAdmin = StatutValidation.EN_ATTENTE;
        }

        if (this.statutResponsableStages == null) {
            this.statutResponsableStages = StatutValidation.EN_ATTENTE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.misAJourLe = LocalDateTime.now();
    }
}
