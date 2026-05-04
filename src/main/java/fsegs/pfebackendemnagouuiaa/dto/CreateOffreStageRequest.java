package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateOffreStageRequest {

    private String titre;
    private String descriptionMissions;
    private Integer duree;
    private String profilRecherche;
    private LocalDate dateDebutPrevue;

    private Long entrepriseId;
    private Long publieeParId;
    private Long valideeParId;
}