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
    private boolean stageCree;
    private boolean stageDeclenche;
    private boolean trelloEnabled;
    private boolean affectable;
}
