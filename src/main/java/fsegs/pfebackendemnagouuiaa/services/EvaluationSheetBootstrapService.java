package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Crée la fiche d'évaluation et la réunion finale associée si nécessaire.
 * Permet à l'encadrant professionnel et au responsable entreprise de démarrer
 * leur partie sans ordre imposé entre les deux rôles.
 */
@Service
@RequiredArgsConstructor
public class EvaluationSheetBootstrapService {

    private static final LocalTime HEURE_REUNION_FINALE_PAR_DEFAUT = LocalTime.of(9, 0);

    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final StageRepository stageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;

    @Transactional
    public FicheEvaluation ensureSheetExists(Long stageId) {
        if (stageId == null) {
            throw new IllegalArgumentException("Le stage est obligatoire.");
        }

        Optional<FicheEvaluation> existing = ficheEvaluationRepository.findFirstByStageId(stageId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable."));
        EvaluationStageAccessRules.ensureEvaluationPeriodOpen(stage);

        ReunionFinale reunionFinale = ensureReunionFinale(stage);

        FicheEvaluation fiche = new FicheEvaluation();
        fiche.setStage(stage);
        fiche.setReunionFinale(reunionFinale);
        fiche.setPointFortEncadrantPro("");
        fiche.setAxeAmeliorationEncadrantPro("");
        fiche.setPointFortResponsableEntreprise("");
        fiche.setAxeAmeliorationResponsableEntreprise("");
        fiche.setNoteFinale(0.0);
        fiche.setSignatures(new ArrayList<>());

        return ficheEvaluationRepository.save(fiche);
    }

    private ReunionFinale ensureReunionFinale(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new EntityNotFoundException("Stage introuvable.");
        }

        ReunionFinale reunionFinale = reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stage.getId())
                .orElse(null);

        if (reunionFinale == null) {
            ReunionFinale created = new ReunionFinale();
            created.setStage(stage);
            created.setCahierStage(stage.getCahierStage());
            created.setNumReunion("RF-" + stage.getId());
            created.setDate(stage.getDateFin());
            created.setHeure(HEURE_REUNION_FINALE_PAR_DEFAUT);
            created.setTypeEncadrantCreateur("SYSTEME");
            created.setNomEncadrantCreateur("SYSTEME");
            created.setParticipants(resolveAutomaticFinalMeetingParticipants(stage));
            return reunionFinaleRepository.save(created);
        }

        boolean changed = false;
        if (!Objects.equals(reunionFinale.getDate(), stage.getDateFin())) {
            reunionFinale.setDate(stage.getDateFin());
            changed = true;
        }
        if (reunionFinale.getHeure() == null) {
            reunionFinale.setHeure(HEURE_REUNION_FINALE_PAR_DEFAUT);
            changed = true;
        }
        if (reunionFinale.getCahierStage() == null && stage.getCahierStage() != null) {
            reunionFinale.setCahierStage(stage.getCahierStage());
            changed = true;
        }
        if (changed) {
            reunionFinale = reunionFinaleRepository.save(reunionFinale);
        }
        return reunionFinale;
    }

    private Set<Utilisateur> resolveAutomaticFinalMeetingParticipants(Stage stage) {
        Set<Utilisateur> participants = new LinkedHashSet<>();
        if (stage == null) {
            return participants;
        }
        if (stage.getStagiaire() != null) {
            participants.add(stage.getStagiaire());
        }
        if (stage.getEncadrantAcademique() != null) {
            participants.add(stage.getEncadrantAcademique());
        }
        if (stage.getEncadrantProfessionnel() != null) {
            participants.add(stage.getEncadrantProfessionnel());
        }
        return participants;
    }
}
