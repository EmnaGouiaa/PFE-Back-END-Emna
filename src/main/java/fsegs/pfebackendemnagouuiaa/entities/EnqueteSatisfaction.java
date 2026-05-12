package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "enquete_satisfaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enquete_satisfaction_stage_utilisateur",
                columnNames = {"stage_id", "utilisateur_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnqueteSatisfaction {

    public static final String TITRE_PAR_DEFAUT = "Enquete de satisfaction";
    public static final String DESCRIPTION_PAR_DEFAUT =
            "Veuillez repondre a l'enquete de satisfaction liee a votre reunion finale.";
    public static final String URL_FORMULAIRE_PAR_DEFAUT = "";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateSoumission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutEnqueteSatisfaction statutEnquete;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column
    private String urlFormulaire;

    @Column(columnDefinition = "LONGTEXT")
    private String reponses;

    @Column(columnDefinition = "TEXT")
    private String commentaireGlobal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private Role roleRepondant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    @JsonIgnore
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonIgnore
    private Utilisateur utilisateur;

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (statutEnquete == null) {
            statutEnquete = StatutEnqueteSatisfaction.EN_ATTENTE;
        }
        if (titre == null || titre.trim().isEmpty()) {
            titre = TITRE_PAR_DEFAUT;
        }
        if (description == null || description.trim().isEmpty()) {
            description = DESCRIPTION_PAR_DEFAUT;
        }
        if (urlFormulaire == null) {
            urlFormulaire = URL_FORMULAIRE_PAR_DEFAUT;
        }
    }
}
