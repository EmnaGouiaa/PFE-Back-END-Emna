package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcule {@code noteFinale} sur l'échelle /5 pour les fiches existantes (migration depuis /20).
 */
@Component
@Order(91)
@RequiredArgsConstructor
@Slf4j
public class EvaluationNoteFinaleRecalculationRunner implements ApplicationRunner {

    private final FicheEvaluationRepository ficheEvaluationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = 0;
        for (FicheEvaluation fiche : ficheEvaluationRepository.findAll()) {
            if (fiche.getId() == null) {
                continue;
            }
            // Initialiser la collection gérée par Hibernate (ne pas remplacer la référence : orphanRemoval).
            fiche.getNotesAttribuees().size();
            double computed = fiche.calculerNoteFinale();
            Double current = fiche.getNoteFinale();
            if (current == null || Math.abs(current - computed) > 0.05) {
                fiche.setNoteFinale(computed);
                ficheEvaluationRepository.save(fiche);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[EVAL-SCORE] {} fiche(s) d'evaluation recalculee(s) sur l'echelle /5.", updated);
        }
    }
}
