package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Configuration globale de l'enquête de satisfaction.
 * Il existe une seule ligne dans cette table (id = 1).
 * Le RESPONSABLE_STAGE gère cette configuration.
 */
@Entity
@Table(name = "configuration_globale_enquete")
@Getter
@Setter
@NoArgsConstructor
public class ConfigurationGlobaleEnquete {

    /** Identifiant fixe — il n'y a qu'une seule configuration dans l'application. */
    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String titre = "Enquête de satisfaction";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description = "Merci de répondre à cette enquête de satisfaction.";

    @Column(length = 512)
    private String urlFormulaire;

    /** Indique si l'enquête est active (visible par les stagiaires/encadrants). */
    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private LocalDateTime dateModification;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        dateModification = LocalDateTime.now();
        if (titre == null || titre.isBlank()) {
            titre = "Enquête de satisfaction";
        }
        if (description == null || description.isBlank()) {
            description = "Merci de répondre à cette enquête de satisfaction.";
        }
        // Sanitize URL
        if (urlFormulaire != null) {
            String trimmed = urlFormulaire.trim();
            urlFormulaire = trimmed.isEmpty() || "string".equalsIgnoreCase(trimmed) ? null : trimmed;
        }
    }
}
