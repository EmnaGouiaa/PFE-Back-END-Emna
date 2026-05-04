package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonIgnore
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(nullable = false)
    private LocalDateTime creeLe;

    @Builder.Default
    @Column(nullable = false)
    private Boolean lu = false;

    private LocalDateTime luLe;

    private String type;

    private Long entiteId;

    private String entiteType;

    @PrePersist
    public void prePersist() {
        if (creeLe == null) {
            creeLe = LocalDateTime.now();
        }
        if (lu == null) {
            lu = false;
        }
    }
}
