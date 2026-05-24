package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class OffreStageResponse {

    private Long id;
    private String titre;
    private String descriptionMissions;
    private Integer duree;
    private String profilRecherche;
    private LocalDate dateDebutPrevue;
    private LocalDate datePublication;
    private String statut;
    private String motifRefus;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long publieeParId;
    private String publieeParNomComplet;
    private Long valideeParId;
    private String valideeParNomComplet;
    private Long encadrantProId;
    private String encadrantProNomComplet;
    private boolean stageCree;
    private boolean stageDeclenche;
    private boolean trelloEnabled;
    private boolean affectable;
    /**
     * Vrai si un etudiant est actuellement affecte (stage lie existant ET non annule).
     * Plus precis que stageCree qui inclut les stages annules.
     */
    private boolean affectationActive;
    /**
     * Statut de validation du sujet du stage lie (EN_ATTENTE, VALIDEE, REFUSEE) ou null
     * s'il n'y a pas d'affectation active. Permet au frontend d'appliquer la regle
     * "edit limite a la description tant que le sujet n'est pas valide".
     */
    private String statutSujet;
}
