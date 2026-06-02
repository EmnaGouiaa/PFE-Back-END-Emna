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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lien entre une {@link Notification} et un {@link Utilisateur} destinataire :
 * état de lecture, action attendue et réponse éventuelle.
 *
 * <h3>Mapping JPA</h3>
 * Table {@code notification_destinataire} avec contrainte d'unicité
 * {@code (notification_id, utilisateur_id)}.
 *
 * <h3>Consommation applicative</h3>
 * Service {@code NotificationServiceImpl} ; contrôleur {@code NotificationController}.
 */
@Entity
@Table(
        name = "notification_destinataire",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_destinataire_notification_utilisateur",
                columnNames = {"notification_id", "utilisateur_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDestinataire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(length = 2000)
    private String reponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_action_notif", nullable = false)
    private StatutActionNotification statutActionNotif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_notification", nullable = false)
    private StatutNotification statutNotification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    @JsonIgnore
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonIgnore
    private Utilisateur utilisateur;
}
