package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    private List<ResponsableEntreprise> responsables = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval= true)
    @JsonIgnore
    private List<OffreStage> offresStage = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<EncadrantProfessionnel> encadrantsProfessionnels = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "entreprise",cascade=CascadeType.ALL,orphanRemoval= true)
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    public void ajouterResponsable(ResponsableEntreprise responsable) {
        responsable.setEntreprise(this);
        this.responsables.add(responsable);
    }

    public void ajouterEncadrantPro(EncadrantProfessionnel encadrant) {
        encadrant.setEntreprise(this);
        this.encadrantsProfessionnels.add(encadrant);
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
