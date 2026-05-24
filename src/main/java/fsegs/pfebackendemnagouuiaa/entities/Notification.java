package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 1200)
    private String body;

    @Column(name = "date_creation", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 100)
    private TypeNotification typeNotification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reunion_id")
    private Reunion reunion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reunion_finale_id")
    private ReunionFinale reunionFinale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reunion_hebdomadaire_id")
    private ReunionHebdomadaire reunionHebdomadaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cahier_stage_id")
    private CahierStage cahierStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_evaluation_id")
    private FicheEvaluation ficheEvaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convention_id")
    private ConventionStage conventionStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private DemandeCreationCompteEntreprise demandeCreationCompteEntreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_notification_id")
    private Notification parentNotification;

    @Builder.Default
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationDestinataire> notificationDestinataires = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
