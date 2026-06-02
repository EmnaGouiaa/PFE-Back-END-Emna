package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenu complet du cahier de stage pour affichage UI (hors PDF binaire).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStageContenuDto {

    private CahierStageDto cahier;
    private CahierStageStageInfoDto informationsGenerales = new CahierStageStageInfoDto();
    private List<CahierStageReunionItemDto> reunionsHebdomadaires = new ArrayList<>();
    private Map<String, List<CahierStageTacheTrelloDto>> tachesTrelloParColonne = new LinkedHashMap<>();
    private boolean trelloSynchronise;
    private List<CahierStageAbsenceItemDto> absences = new ArrayList<>();
    private List<String> raisonsPdfIndisponible = new ArrayList<>();
    private boolean pdfDisponible;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CahierStageStageInfoDto {
        private String titre;
        private String sujet;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private String stagiaireNom;
        private String stagiaireEmail;
        private String encadrantAcademiqueNom;
        private String encadrantAcademiqueEmail;
        private String encadrantProfessionnelNom;
        private String encadrantProfessionnelEmail;
        private String entrepriseNom;
        private String entrepriseEmail;
        private String entrepriseTelephone;
        private String entrepriseSecteur;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CahierStageReunionItemDto {
        private Long id;
        private String numReunion;
        private LocalDate date;
        private String heure;
        private String observationEncadrant;
        private String typeEncadrantCreateur;
        private String nomEncadrantCreateur;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CahierStageTacheTrelloDto {
        private String nom;
        private String description;
        private String liste;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CahierStageAbsenceItemDto {
        private Long id;
        private LocalDate date;
        private Integer nombreJours;
        private String statut;
        private String justification;
        private String commentaire;
    }
}
