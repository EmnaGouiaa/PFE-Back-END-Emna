package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto.CahierStageAbsenceItemDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto.CahierStageReunionItemDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto.CahierStageStageInfoDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageContenuDto.CahierStageTacheTrelloDto;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentsOverviewDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.AbsenceRepository;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.service.LogbookMeetingSupport;
import fsegs.pfebackendemnagouuiaa.service.LogbookTrelloSupport;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CahierStageContenuServiceImpl implements CahierStageContenuService {

    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final ReunionRepository reunionRepository;
    private final AbsenceRepository absenceRepository;
    private final CahierStageService cahierStageService;
    private final StageDocumentService stageDocumentService;
    private final StageService stageService;

    @Override
    public CahierStageContenuDto getContenuByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + stageId));

        CahierStageContenuDto contenu = new CahierStageContenuDto();
        contenu.setInformationsGenerales(buildStageInfo(stage));

        CahierStageDto cahierDto = cahierStageRepository.findByStageId(stageId)
                .map(cahier -> cahierStageService.getById(cahier.getId()))
                .orElse(null);
        contenu.setCahier(cahierDto);

        contenu.setReunionsHebdomadaires(buildReunions(stageId));
        fillTrello(contenu, stage);
        contenu.setAbsences(buildAbsences(stageId));

        StageDocumentsOverviewDto overview = stageDocumentService.getStageDocuments(stageId);
        if (overview != null && overview.getCahierStage() != null) {
            contenu.setPdfDisponible(Boolean.TRUE.equals(overview.getCahierStage().getDisponible()));
            String raison = overview.getCahierStage().getRaisonAbsence();
            if (raison != null && !raison.isBlank()) {
                contenu.setRaisonsPdfIndisponible(List.of(raison.trim()));
            }
        } else if (cahierDto == null) {
            contenu.setPdfDisponible(false);
            contenu.setRaisonsPdfIndisponible(List.of("Le cahier de stage n'existe pas encore."));
        }

        return contenu;
    }

    private CahierStageStageInfoDto buildStageInfo(Stage stage) {
        CahierStageStageInfoDto info = new CahierStageStageInfoDto();
        info.setTitre(stage.getTitre());
        info.setSujet(stage.getSujet());
        info.setDateDebut(stage.getDateDebut());
        info.setDateFin(stage.getDateFin());
        if (stage.getStagiaire() != null) {
            info.setStagiaireNom(fullName(stage.getStagiaire()));
            info.setStagiaireEmail(stage.getStagiaire().getEmail());
        }
        if (stage.getEncadrantAcademique() != null) {
            info.setEncadrantAcademiqueNom(fullName(stage.getEncadrantAcademique()));
            info.setEncadrantAcademiqueEmail(stage.getEncadrantAcademique().getEmail());
        }
        if (stage.getEncadrantProfessionnel() != null) {
            info.setEncadrantProfessionnelNom(fullName(stage.getEncadrantProfessionnel()));
            info.setEncadrantProfessionnelEmail(stage.getEncadrantProfessionnel().getEmail());
        }
        if (stage.getEntreprise() != null) {
            info.setEntrepriseNom(stage.getEntreprise().getNom());
            info.setEntrepriseEmail(stage.getEntreprise().getEmail());
            info.setEntrepriseTelephone(stage.getEntreprise().getTelephone());
            info.setEntrepriseSecteur(stage.getEntreprise().getSecteurActivite());
        }
        return info;
    }

    private List<CahierStageReunionItemDto> buildReunions(Long stageId) {
        List<Reunion> reunions = reunionRepository.findByStageId(stageId);
        return LogbookMeetingSupport.weeklyMeetingsSorted(reunions)
                .map(reunion -> new CahierStageReunionItemDto(
                        reunion.getId(),
                        reunion.getNumReunion(),
                        reunion.getDate(),
                        reunion.getHeure() != null ? reunion.getHeure().toString() : "",
                        LogbookMeetingSupport.resolveCreatorObservation(reunion),
                        reunion.getTypeEncadrantCreateur(),
                        reunion.getNomEncadrantCreateur()
                ))
                .collect(Collectors.toList());
    }

    private void fillTrello(CahierStageContenuDto contenu, Stage stage) {
        Map<String, List<CahierStageTacheTrelloDto>> columns = new LinkedHashMap<>();
        columns.put(LogbookTrelloSupport.COLUMN_TODO, new ArrayList<>());
        columns.put(LogbookTrelloSupport.COLUMN_IN_PROGRESS, new ArrayList<>());
        columns.put(LogbookTrelloSupport.COLUMN_DONE, new ArrayList<>());

        try {
            Map<String, Object> resume = stageService.getResumeTrelloStage(stage.getId());
            boolean synchronise = Boolean.TRUE.equals(resume.get("trelloDisponible"));
            contenu.setTrelloSynchronise(synchronise);
            if (!synchronise) {
                contenu.setTachesTrelloParColonne(columns);
                return;
            }

            columns.put(LogbookTrelloSupport.COLUMN_TODO, mapTrelloTasks(resume.get("tachesAFaire")));
            columns.put(LogbookTrelloSupport.COLUMN_IN_PROGRESS, mapTrelloTasks(resume.get("tachesEnCours")));
            columns.put(LogbookTrelloSupport.COLUMN_DONE, mapTrelloTasks(resume.get("tachesTerminees")));
        } catch (RuntimeException ex) {
            contenu.setTrelloSynchronise(false);
        }
        contenu.setTachesTrelloParColonne(columns);
    }

    @SuppressWarnings("unchecked")
    private List<CahierStageTacheTrelloDto> mapTrelloTasks(Object rawTasks) {
        if (!(rawTasks instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<CahierStageTacheTrelloDto> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> card)) {
                continue;
            }
            Object desc = card.get("desc");
            result.add(new CahierStageTacheTrelloDto(
                    String.valueOf(card.get("name")),
                    desc != null ? String.valueOf(desc) : "",
                    ""
            ));
        }
        return result;
    }

    private List<CahierStageAbsenceItemDto> buildAbsences(Long stageId) {
        return absenceRepository.findByStageId(stageId).stream()
                .sorted(Comparator.comparing(Absence::getDateAbsence, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(absence -> new CahierStageAbsenceItemDto(
                        absence.getId(),
                        absence.getDateAbsence(),
                        absence.getNbAbsence(),
                        absence.getStatut(),
                        absence.getJustification(),
                        absence.getCommentaire()
                ))
                .toList();
    }

    private String fullName(Utilisateur user) {
        if (user == null) {
            return "—";
        }
        String name = ((user.getPrenom() != null ? user.getPrenom() : "") + " "
                + (user.getNom() != null ? user.getNom() : "")).trim();
        return name.isBlank() ? "—" : name;
    }
}
