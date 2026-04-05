package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffreStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(length = 2000)
    private String description;

    private String missions;
    private String competencesRequises;
    private Integer duree;


    private LocalDate dateDebut;

    private Double gratification;


    private LocalDate datePublication;

    @Enumerated(EnumType.STRING)
    private StatutOffre statut;

    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable=false,name="entreprise")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publiee_par_id")
    private ResponsableEntreprise publieePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validee_par_id")
    private ResponsableServiceStages valideePar;

    @OneToOne(mappedBy = "offreSource")
    @JsonIgnore

    private Stage stage;
    public void soumettreValidation() {
        this.statut = StatutOffre.EN_ATTENTE;
    }

    public void valider(ResponsableServiceStages gestionnaire) {
        this.valideePar = gestionnaire;
        this.statut = StatutOffre.VALIDEE;
    }

    public void rejeter(String motif) {
        this.motifRejet = motif;
        this.statut = StatutOffre.REJETEE;
    }

    public void marquerPourvue() {
        this.statut = StatutOffre.POURVUE;
    }


}
