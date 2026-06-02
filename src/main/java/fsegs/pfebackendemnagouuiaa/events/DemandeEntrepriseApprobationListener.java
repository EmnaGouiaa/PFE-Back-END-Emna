package fsegs.pfebackendemnagouuiaa.events;

import fsegs.pfebackendemnagouuiaa.services.DemandeCreationCompteEntrepriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemandeEntrepriseApprobationListener {

    private final DemandeCreationCompteEntrepriseService demandeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDemandeApprouvee(DemandeEntrepriseApprouveeEvent event) {
        if (event == null || event.demandeId() == null) {
            return;
        }
        try {
            demandeService.finaliserCreationCompteApresApprobation(event.demandeId());
        } catch (RuntimeException ex) {
            log.error(
                    "[DemandeApprobationListener] Finalisation asynchrone echouee pour la demande {} : {}",
                    event.demandeId(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
