package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("FINALE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"ficheEvaluation"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReunionFinale extends Reunion {

    private Integer note;

    @Column(nullable = true)
    private String urlFormEvaluation;

    @Column(nullable = true)
    private String urlFormSatisfaction;

    private String titreEnqueteSatisfaction;

    @Column(columnDefinition = "TEXT")
    private String descriptionEnqueteSatisfaction;

    @OneToMany(mappedBy = "reunionFinale", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<FicheEvaluation> ficheEvaluation = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void sanitizeFormUrls() {
        this.urlFormEvaluation = sanitizeUrlValue(this.urlFormEvaluation);
        this.urlFormSatisfaction = sanitizeUrlValue(this.urlFormSatisfaction);
    }

    private String sanitizeUrlValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "string".equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }
}
