package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.services.EvaluationStageAccessRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Nettoie les fiches d'evaluation associees a des stages non termines (donnees incoherentes legacy / seed).
 */
@Component
@Order(90)
@RequiredArgsConstructor
@Slf4j
public class EvaluationEnCoursSanitizerRunner implements ApplicationRunner {

    private final StageRepository stageRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final NoteAttribueeRepository noteAttribueeRepository;
    private final CritereEvaluationRepository critereEvaluationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int sanitized = 0;
        for (Stage stage : stageRepository.findAll()) {
            if (stage == null || stage.getId() == null) {
                continue;
            }
            if (EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
                continue;
            }
            if (stage.getStatut() == StatutStage.ANNULE || stage.getStatut() == StatutStage.REFUSE) {
                continue;
            }
            var ficheOpt = ficheEvaluationRepository.findFirstByStageId(stage.getId());
            if (ficheOpt.isEmpty()) {
                if (Boolean.TRUE.equals(stage.getSectionEvaluationOuverte())) {
                    stage.setSectionEvaluationOuverte(Boolean.FALSE);
                    stageRepository.save(stage);
                }
                continue;
            }
            resetFicheContent(ficheOpt.get());
            stage.setSectionEvaluationOuverte(Boolean.FALSE);
            stageRepository.save(stage);
            sanitized++;
        }
        if (sanitized > 0) {
            log.info("[EVAL-SANITIZE] {} fiche(s) d'evaluation reinitialisee(s) avant la date de fin du stage.", sanitized);
        }
    }

    private void resetFicheContent(FicheEvaluation fiche) {
        Long ficheId = fiche.getId();
        if (ficheId == null) {
            return;
        }

        List<NoteAttribuee> notes = noteAttribueeRepository.findByFicheEvaluationId(ficheId);
        if (!notes.isEmpty()) {
            noteAttribueeRepository.deleteAll(notes);
        }

        List<CritereEvaluation> criteres = critereEvaluationRepository.findByFicheId(ficheId);
        if (!criteres.isEmpty()) {
            critereEvaluationRepository.deleteAll(criteres);
        }

        if (fiche.getSignatures() != null) {
            fiche.getSignatures().clear();
        } else {
            fiche.setSignatures(new ArrayList<>());
        }

        fiche.setPointFortEncadrantPro("");
        fiche.setAxeAmeliorationEncadrantPro("");
        fiche.setPointFortResponsableEntreprise("");
        fiche.setAxeAmeliorationResponsableEntreprise("");
        fiche.setNoteFinale(0.0);
        ficheEvaluationRepository.save(fiche);
    }
}
