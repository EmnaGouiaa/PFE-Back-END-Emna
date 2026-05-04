package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String adresse;

    private String email;

    private String telephone;

    private String secteurActivite;
    @Builder.Default
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval= true)
    @JsonIgnore
    private List<ResponsableEntreprise> tuteurs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval= true)
    @JsonIgnore
    private List<OffreStage> offresStage = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<EncadrantProfessionnel> encadrantProfessionnels = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise",cascade=CascadeType.ALL,orphanRemoval= true)
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    public void ajouterTuteur(ResponsableEntreprise tuteur) {
        tuteur.setEntreprise(this);
        this.tuteurs.add(tuteur);
    }

    public void ajouterEncadrantPro(EncadrantProfessionnel encadrant) {
        encadrant.setEntreprise(this);
        this.encadrantProfessionnels.add(encadrant);
    }

    public void publierOffre(OffreStage offre) {
        offre.setEntreprise(this);
        this.offresStage.add(offre);
    }
    public void addStage(Stage stage) {
        this.stages.add(stage);
        stage.setEntreprise(this);
    }
    public void removeStage(Stage stage) {
        this.stages.remove(stage);
        stage.setEntreprise(null);
    }

}
