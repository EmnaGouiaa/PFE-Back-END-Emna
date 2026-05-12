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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "rapport_enquete_satisfaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rapport_enquete_stage",
                columnNames = {"stage_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RapportEnqueteSatisfaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false, length = 1200)
    private String cheminFichier;

    @Column(nullable = false)
    private LocalDateTime dateUpload;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    @JsonIgnore
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @JsonIgnore
    private Utilisateur uploadedBy;

    @PrePersist
    public void prePersist() {
        if (dateUpload == null) {
            dateUpload = LocalDateTime.now();
        }
    }
}
