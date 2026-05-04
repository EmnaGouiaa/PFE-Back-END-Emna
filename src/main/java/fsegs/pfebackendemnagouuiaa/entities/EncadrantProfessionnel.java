package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@DiscriminatorValue("EncadrantProfessionnel")
public class EncadrantProfessionnel extends Utilisateur {

    private String poste;

    private String service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"tuteurs"})
    private Entreprise entreprise;

    @OneToMany(mappedBy = "encadrantProfessionnel")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @PrePersist
    private void applyDefaults() {
        if (getRole() == null) {
            setRole(Role.ENCADRANT_PROFESSIONNEL);
        }
        if (getActif() == null) {
            setActif(true);
        }
    }

}
