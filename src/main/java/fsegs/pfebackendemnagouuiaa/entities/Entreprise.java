package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Organisation hôte des stages : coordonnées, responsables, offres et encadrants professionnels.
 *
 * <h3>Mapping JPA</h3>
 * Table {@code entreprise}. Collections {@code @OneToMany} avec cascade {@code ALL} et
 * {@code orphanRemoval} pour maintenir la cohérence bidirectionnelle (responsables, offres,
 * encadrants, stages).
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Services : {@code OffreStageServiceImpl}, {@code DemandeCreationCompteEntrepriseServiceImpl},
 *       {@code AdminCompanyAccountServiceImpl}, {@code CompanyValidationServiceImpl}.</li>
 *   <li>Contrôleurs : {@code EntrepriseController}, {@code AdminCompanyAccountController}.</li>
 * </ul>
 */
@Entity
@Data
@EqualsAndHashCode(exclude = {
        "responsables",
        "offresStage",
        "encadrantsProfessionnels",
        "stages"
})
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

    /** Associe un responsable à cette entreprise (côté propriétaire de la relation). */
    public void ajouterResponsable(ResponsableEntreprise responsable) {
        responsable.setEntreprise(this);
        this.responsables.add(responsable);
    }

    /** Associe un encadrant professionnel à cette entreprise. */
    public void ajouterEncadrantPro(EncadrantProfessionnel encadrant) {
        encadrant.setEntreprise(this);
        this.encadrantsProfessionnels.add(encadrant);
    }

    /** Rattache une offre à cette entreprise dans la collection {@link #offresStage}. */
    public void publierOffre(OffreStage offre) {
        offre.setEntreprise(this);
        this.offresStage.add(offre);
    }
    /** Ajoute un stage et synchronise la FK {@code entreprise} côté {@link Stage}. */
    public void addStage(Stage stage) {
        this.stages.add(stage);
        stage.setEntreprise(this);
    }
    /** Retire un stage de la collection et dissocie l'entreprise côté entité enfant. */
    public void removeStage(Stage stage) {
        this.stages.remove(stage);
        stage.setEntreprise(null);
    }

}
