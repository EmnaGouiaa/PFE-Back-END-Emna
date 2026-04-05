package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@ToString(exclude = {"stagiaire", "encadrantAcademique", "tuteurEntreprise",
        "encadrantProfessionnel", "entreprise", "dossierStage", "offreSource"})
public class Stage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sujet;

    @Column(length = 2000)
    private String description;


    private LocalDate dateDebut;

    private Integer dureeSemaines;

    @Enumerated(EnumType.STRING)
    private TypeStage type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_id")
    private Etudiant stagiaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_academique_id")
    private EncadrantAcademique encadrantAcademique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuteur_entreprise_id")
    private ResponsableEntreprise tuteurEntreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_professionnel_id")
    private EncadrantProfessionnel encadrantProfessionnel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable=false,name="entreprise")
    private Entreprise entreprise;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_stage_id")
    @JsonIgnore
    private DossierStage dossierStage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_source_id")
    @JsonIgnore
    private OffreStage offreSource;
    public void demarrer() {
        this.type = TypeStage.EN_COURS;
    }

    public void terminer() {
        this.type = TypeStage.TERMINE;
    }

    public void suspendre(String motif) {
        this.type = TypeStage.SUSPENDU;
    }

}
