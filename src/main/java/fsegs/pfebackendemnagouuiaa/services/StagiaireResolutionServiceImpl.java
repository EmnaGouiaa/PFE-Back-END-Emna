package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StagiaireResolutionServiceImpl implements StagiaireResolutionService {

    private static final Logger log = LoggerFactory.getLogger(StagiaireResolutionServiceImpl.class);

    private final StagiaireRepository stagiaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Stagiaire findByEmail(String email) {
        log.info("Resolution stagiaire - email brut recu: [{}]", email);
        String normalizedEmail = normalizeRequiredEmail(email);
        log.info("Resolution stagiaire - email normalise: [{}]", normalizedEmail);

        return stagiaireRepository.findByNormalizedEmail(normalizedEmail)
                .map(stagiaire -> {
                    log.info(
                            "Resolution stagiaire - stagiaire trouve directement: id={}, email={}, actif={}, classe={}",
                            stagiaire.getId(),
                            stagiaire.getEmail(),
                            stagiaire.getActif(),
                            stagiaire.getClass().getSimpleName()
                    );
                    return ensureActiveStudent(stagiaire);
                })
                .orElseGet(() -> repairAndLoadLegacyStudent(normalizedEmail));
    }

    private Stagiaire repairAndLoadLegacyStudent(String email) {
        log.warn("Resolution stagiaire - aucun Stagiaire direct trouve pour [{}], verification via Utilisateur.", email);
        Utilisateur utilisateur = utilisateurRepository.findByNormalizedEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Etudiant introuvable"));

        log.info(
                "Resolution stagiaire - utilisateur trouve: id={}, email={}, role={}, actif={}, classe={}",
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getRole(),
                utilisateur.getActif(),
                utilisateur.getClass().getSimpleName()
        );

        if (utilisateur.getRole() != Role.STAGIAIRE) {
            log.warn("Resolution stagiaire - utilisateur trouve mais role incompatible: {}", utilisateur.getRole());
            throw new EntityNotFoundException("L'utilisateur correspondant n'est pas un etudiant.");
        }

        if (Boolean.FALSE.equals(utilisateur.getActif())) {
            log.warn("Resolution stagiaire - utilisateur stagiaire trouve mais compte inactif: id={}", utilisateur.getId());
            throw new IllegalStateException("Le compte de cet etudiant est inactif.");
        }

        if (utilisateur instanceof Stagiaire stagiaire) {
            log.info("Resolution stagiaire - utilisateur deja instancie comme Stagiaire: id={}", stagiaire.getId());
            return stagiaire;
        }

        log.warn("Resolution stagiaire - compte legacy detecte pour {} (id={}). Reparation stagiaire + dtype.", email, utilisateur.getId());

        entityManager.createNativeQuery("""
                INSERT INTO stagiaire (id)
                SELECT :id
                WHERE NOT EXISTS (SELECT 1 FROM stagiaire WHERE id = :id)
                """)
                .setParameter("id", utilisateur.getId())
                .executeUpdate();

        entityManager.createNativeQuery("""
                UPDATE utilisateur
                SET dtype = 'Stagiaire'
                WHERE id = :id AND (dtype IS NULL OR dtype <> 'Stagiaire')
                """)
                .setParameter("id", utilisateur.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        Number utilisateurRowCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM utilisateur WHERE id = :id
                """)
                .setParameter("id", utilisateur.getId())
                .getSingleResult();

        Number stagiaireRowCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM stagiaire WHERE id = :id
                """)
                .setParameter("id", utilisateur.getId())
                .getSingleResult();

        Object dtypeValue = entityManager.createNativeQuery("""
                SELECT dtype FROM utilisateur WHERE id = :id
                """)
                .setParameter("id", utilisateur.getId())
                .getSingleResult();

        log.info(
                "Resolution stagiaire - verification post-reparation: utilisateurId={}, lignes utilisateur={}, lignes stagiaire={}, dtype={}",
                utilisateur.getId(),
                utilisateurRowCount,
                stagiaireRowCount
                ,
                dtypeValue
        );

        if (utilisateurRowCount.longValue() == 0L || stagiaireRowCount.longValue() == 0L) {
            throw new EntityNotFoundException("Le compte etudiant existe, mais sa fiche stagiaire n'est pas encore initialisee.");
        }

        Stagiaire stagiaire = stagiaireRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ce compte existe mais sa fiche stagiaire n'est pas initialisee."));

        log.info(
                "Resolution stagiaire - stagiaire recharge apres reparation: id={}, classe={}",
                utilisateur.getId(),
                stagiaire.getClass().getSimpleName()
        );
        return ensureActiveStudent(stagiaire);
    }

    private Stagiaire ensureActiveStudent(Stagiaire stagiaire) {
        if (Boolean.FALSE.equals(stagiaire.getActif())) {
            log.warn("Resolution stagiaire - stagiaire trouve mais inactif: id={}", stagiaire.getId());
            throw new IllegalStateException("Le compte de ce stagiaire est inactif.");
        }

        return stagiaire;
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("L'email de l'etudiant est obligatoire.");
        }

        String normalized = email.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("L'email de l'etudiant est obligatoire.");
        }

        return normalized;
    }
}
