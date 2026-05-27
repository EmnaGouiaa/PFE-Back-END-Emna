package fsegs.pfebackendemnagouuiaa.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemandeEntrepriseCommunicationAsyncService {

    private final NotificationService notificationService;
    private final AccountEmailService accountEmailService;

    @Async
    public void envoyerCreationCompteResponsableAsync(
            String prenom,
            String email,
            String motDePasseTemporaire,
            String nomEntreprise
    ) {
        try {
            notificationService.notifierCreationCompteEntreprise(email, motDePasseTemporaire, nomEntreprise);
        } catch (Exception ex) {
            log.warn("Notification de creation de compte non envoyee pour {} : {}", email, ex.getMessage());
        }

        try {
            accountEmailService.sendAccountCreatedEmail(prenom, email, motDePasseTemporaire);
        } catch (Exception ex) {
            log.warn("Email de creation de compte non envoye pour {} : {}", email, ex.getMessage());
        }
    }
}
