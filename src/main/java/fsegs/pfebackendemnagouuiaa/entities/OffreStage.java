package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffreStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String descriptionMissions;

    private Integer duree;

    private String profilRecherche;

    private LocalDate dateDebutPrevue;

    private LocalDate datePublication;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private StatutOffre statut;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "entreprise_id")
    @JsonIgnore
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publiee_par_id")
    @JsonIgnore
    private ResponsableEntreprise publieePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validee_par_id")
    @JsonIgnore
    private ResponsableServiceStages valideePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stagiaire_affecte_id")
    @JsonIgnore
    private Stagiaire stagiaireAffecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_pro_id")
    @JsonIgnoreProperties({"stages", "hibernateLazyInitializer", "handler"})
    private EncadrantProfessionnel encadrantPro;

    @OneToMany(mappedBy = "offreSource")
    @JsonIgnore
    private Set<Stage> stages = new HashSet<>();
}
