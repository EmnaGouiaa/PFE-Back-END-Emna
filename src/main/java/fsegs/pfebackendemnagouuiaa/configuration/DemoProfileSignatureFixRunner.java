package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.services.DemoSigningSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garantit des signatures de profil pour les comptes de demonstration (independant du seed complet).
 */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class DemoProfileSignatureFixRunner implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public void run(String... args) {
        int patched = 0;
        for (Utilisateur utilisateur : utilisateurRepository.findAll()) {
            if (!DemoSigningSupport.isDemoAccountEmail(utilisateur.getEmail())) {
                continue;
            }
            if (utilisateur.getUrlSignature() != null && !utilisateur.getUrlSignature().isBlank()) {
                continue;
            }
            utilisateur.setUrlSignature(DemoSigningSupport.DEMO_SIGNATURE_DATA_URL);
            utilisateurRepository.save(utilisateur);
            patched++;
        }
        if (patched > 0) {
            log.info("[DEMO-SIGN] Signatures de profil ajoutees pour {} compte(s) de demonstration.", patched);
        }
    }
}
