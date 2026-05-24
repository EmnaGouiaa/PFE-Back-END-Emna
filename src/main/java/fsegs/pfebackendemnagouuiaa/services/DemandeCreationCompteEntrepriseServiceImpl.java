package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.StatutDemande;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.entities.TypeNotification;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.DemandeCreationCompteEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemandeCreationCompteEntrepriseServiceImpl implements DemandeCreationCompteEntrepriseService {

    private final DemandeCreationCompteEntrepriseRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactUniquenessService contactUniquenessService;
    private final NotificationService notificationService;
    private final AccountEmailService accountEmailService;
    private final JwtService jwtService;

    @Override
    public DemandeCreationCompteEntreprise createDemande(CreateDemandeCreationCompteEntrepriseRequest request) {
        Long stagiaireId = resolveAuthorizedStagiaireId(request.getStagiaireId());
        Utilisateur stagiaire = utilisateurRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));
        NormalizedDemandeRequest normalized = normalizeAndValidateRequest(request);

        DemandeCreationCompteEntreprise demande = DemandeCreationCompteEntreprise.builder()
                .stagiaire(stagiaire)
                .nomEntreprise(normalized.nomEntreprise())
                .emailEntreprise(normalized.emailEntreprise())
                .telephoneEntreprise(normalized.telephoneEntreprise())
                .adresse(normalized.adresse())
                .secteurActivite(normalized.secteurActivite())
                .nomResponsable(normalized.nomResponsable())
                .prenomResponsable(normalized.prenomResponsable())
                .emailResponsable(normalized.emailResponsable())
                .telephoneResponsable(normalized.telephoneResponsable())
                .build();

        DemandeCreationCompteEntreprise saved = demandeRepository.save(demande);
        try {
            notifierResponsableUniversitaireNouvelleDemande(saved);
        } catch (Exception ex) {
            log.warn("Notification non envoyee pour la soumission de la demande {} : {}", saved.getId(), ex.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public DemandeCreationCompteEntreprise updateDemande(Long id, CreateDemandeCreationCompteEntrepriseRequest request) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        authorizeDemandeOwnerIfNeeded(demande);
        ensureDemandeEditable(demande);

        Long stagiaireId = resolveAuthorizedStagiaireId(request.getStagiaireId());
        Utilisateur stagiaire = utilisateurRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));
        NormalizedDemandeRequest normalized = normalizeAndValidateRequest(request);

        demande.setStagiaire(stagiaire);
        demande.setNomEntreprise(normalized.nomEntreprise());
        demande.setEmailEntreprise(normalized.emailEntreprise());
        demande.setTelephoneEntreprise(normalized.telephoneEntreprise());
        demande.setAdresse(normalized.adresse());
        demande.setSecteurActivite(normalized.secteurActivite());
        demande.setNomResponsable(normalized.nomResponsable());
        demande.setPrenomResponsable(normalized.prenomResponsable());
        demande.setEmailResponsable(normalized.emailResponsable());
        demande.setTelephoneResponsable(normalized.telephoneResponsable());

        return demandeRepository.save(demande);
    }

    @Override
    public DemandeCreationCompteEntreprise getDemandeById(Long id) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        authorizeDemandeOwnerIfNeeded(demande);
        return demande;
    }

    @Override
    public List<DemandeCreationCompteEntreprise> getAllDemandes() {
        return demandeRepository.findAll();
    }

    @Override
    public List<DemandeCreationCompteEntreprise> getDemandesByStagiaire(Long stagiaireId) {
        authorizeStagiaireAccess(stagiaireId);
        return demandeRepository.findByStagiaireId(stagiaireId);
    }

    @Override
    @Transactional
    public DemandeCreationCompteEntreprise validerParResponsableStages(Long demandeId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        // Idempotence : si déjà validée par le responsable, retour sans re-créer le compte.
        if (demande.getStatutResponsableStages() == StatutValidation.VALIDEE) {
            log.info("[DemandeService] validerParResponsableStages: demande {} deja approuvee — retour idempotent.", demandeId);
            return demande;
        }

        ensureDemandeStillActionable(demande);

        // Vérification AVANT le save pour éviter un rollback inutile de la mise à jour du statut.
        verifierCreationCompteResponsablePossible(demande);

        demande.setStatutResponsableStages(StatutValidation.VALIDEE);
        mettreAJourStatutGlobal(demande);

        DemandeCreationCompteEntreprise saved = demandeRepository.save(demande);
        finaliserCreationCompteEntrepriseSiApprouvee(saved);
        return saved;
    }

    private void verifierCreationCompteResponsablePossible(DemandeCreationCompteEntreprise demande) {
        if (demande.getNomEntreprise() == null || demande.getNomEntreprise().isBlank()) {
            throw new IllegalStateException("Le nom de l'entreprise est manquant dans la demande. Veuillez corriger la demande avant de valider.");
        }
        if (demande.getEmailResponsable() == null || demande.getEmailResponsable().isBlank()) {
            throw new IllegalStateException("L'adresse email du responsable est manquante dans la demande. Veuillez corriger la demande avant de valider.");
        }
        if (demande.getNomResponsable() == null || demande.getNomResponsable().isBlank()) {
            throw new IllegalStateException("Le nom du responsable est manquant dans la demande. Veuillez corriger la demande avant de valider.");
        }
        if (demande.getPrenomResponsable() == null || demande.getPrenomResponsable().isBlank()) {
            throw new IllegalStateException("Le prenom du responsable est manquant dans la demande. Veuillez corriger la demande avant de valider.");
        }

        Utilisateur existingByEmail = utilisateurRepository.findByEmailIgnoreCase(demande.getEmailResponsable()).orElse(null);
        if (existingByEmail != null && !(existingByEmail instanceof ResponsableEntreprise)) {
            throw new IllegalStateException(
                "Un compte utilisateur existe deja avec l'adresse email du responsable : "
                + demande.getEmailResponsable()
                + ". Impossible de creer un compte entreprise avec cet email."
            );
        }

        if (demande.getTelephoneResponsable() != null && !demande.getTelephoneResponsable().isBlank()) {
            utilisateurRepository.findByTelephone(demande.getTelephoneResponsable()).ifPresent(existing -> {
                if (!(existing instanceof ResponsableEntreprise)) {
                    throw new IllegalStateException(
                        "Un compte utilisateur existe deja avec le numero de telephone du responsable : "
                        + demande.getTelephoneResponsable()
                        + ". Impossible de creer un compte entreprise avec ce telephone."
                    );
                }
            });
        }
    }

    @Override
    public DemandeCreationCompteEntreprise refuserParResponsableStages(Long demandeId, String commentaire) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);
        String commentaireNormalise = normalizeRequiredComment(commentaire);

        demande.setStatutResponsableStages(StatutValidation.REFUSEE);
        demande.setCommentaireResponsableStages(commentaireNormalise);
        demande.setStatut(StatutDemande.REFUSEE);
        DemandeCreationCompteEntreprise saved = demandeRepository.save(demande);
        notifierRefusDemande(saved, commentaireNormalise);
        return saved;
    }

    private void mettreAJourStatutGlobal(DemandeCreationCompteEntreprise demande) {
        StatutValidation resp = demande.getStatutResponsableStages();
        if (resp == StatutValidation.VALIDEE) {
            demande.setStatut(StatutDemande.VALIDEE);
        } else if (resp == StatutValidation.REFUSEE) {
            demande.setStatut(StatutDemande.REFUSEE);
        } else {
            demande.setStatut(StatutDemande.EN_ATTENTE);
        }
    }

    private void finaliserCreationCompteEntrepriseSiApprouvee(DemandeCreationCompteEntreprise demande) {
        if (demande == null || demande.getId() == null) return;
        if (demande.getStatut() != StatutDemande.VALIDEE) return;

        creerEntrepriseDepuisDemandeSiNecessaire(demande);
        Long stagiaireId = demande.getStagiaire() != null ? demande.getStagiaire().getId() : null;
        if (stagiaireId != null) {
            try {
                notificationService.notifierDemandeEntrepriseValidee(stagiaireId, demande.getId(), demande.getNomEntreprise());
            } catch (Exception ex) {
                log.warn("Notification d'approbation non envoyee pour la demande {} : {}", demande.getId(), ex.getMessage());
            }
        }
    }

    private void notifierResponsableUniversitaireNouvelleDemande(DemandeCreationCompteEntreprise demande) {
        if (demande == null || demande.getId() == null) {
            return;
        }

        List<Utilisateur> responsables = utilisateurRepository.findByRole(Role.RESPONSABLE_STAGE);
        if (responsables.isEmpty()) {
            return;
        }

        String nomEntreprise = demande.getNomEntreprise() == null || demande.getNomEntreprise().isBlank()
                ? "une nouvelle entreprise"
                : demande.getNomEntreprise().trim();
        String nomStagiaire = demande.getStagiaire() == null
                ? "Un stagiaire"
                : ((demande.getStagiaire().getPrenom() == null ? "" : demande.getStagiaire().getPrenom().trim()) + " "
                + (demande.getStagiaire().getNom() == null ? "" : demande.getStagiaire().getNom().trim())).trim();

        notificationService.createNotification(
                "Nouvelle demande d'ajout d'entreprise",
                (nomStagiaire.isBlank() ? "Un stagiaire" : nomStagiaire)
                        + " a soumis une demande pour ajouter l'entreprise "
                        + nomEntreprise
                        + ".",
                TypeNotification.VALIDATION_ENTREPRISE,
                responsables
        );
    }

    private void notifierRefusDemande(DemandeCreationCompteEntreprise demande, String motifRefus) {
        if (demande == null || demande.getStagiaire() == null || demande.getStagiaire().getId() == null) {
            return;
        }

        try {
            notificationService.notifierDemandeEntrepriseRefusee(
                    demande.getStagiaire().getId(),
                    demande.getId(),
                    motifRefus
            );
        } catch (Exception ex) {
            log.warn("Notification de refus non envoyee pour la demande {} : {}", demande.getId(), ex.getMessage());
        }
    }

    private void creerEntrepriseDepuisDemandeSiNecessaire(DemandeCreationCompteEntreprise demande) {
        Entreprise entreprise = entrepriseRepository.findByEmailIgnoreCase(demande.getEmailEntreprise())
                .or(() -> entrepriseRepository.findByNomIgnoreCase(demande.getNomEntreprise()))
                .orElse(null);

        if (entreprise == null) {
            entreprise = new Entreprise();
            entreprise.setNom(demande.getNomEntreprise());
            entreprise.setEmail(demande.getEmailEntreprise());
            entreprise.setTelephone(demande.getTelephoneEntreprise());
            entreprise.setAdresse(demande.getAdresse());
            entreprise.setSecteurActivite(demande.getSecteurActivite());
            entreprise = entrepriseRepository.save(entreprise);
        }

        Utilisateur existingUser = utilisateurRepository.findByEmailIgnoreCase(demande.getEmailResponsable()).orElse(null);
        if (existingUser != null) {
            if (existingUser instanceof ResponsableEntreprise) {
                return;
            }
            throw new RuntimeException("Un compte utilisateur existe deja avec cet email");
        }

        String motDePasseTemporaire = genererMotDePasseTemporaire();

        ResponsableEntreprise responsable = ResponsableEntreprise.builder()
                .nom(demande.getNomResponsable())
                .prenom(demande.getPrenomResponsable())
                .email(demande.getEmailResponsable())
                .telephone(demande.getTelephoneResponsable())
                .entreprise(entreprise)
                .role(Role.RESPONSABLE_ENTREPRISE)
                .actif(true)
                .supprime(false)
                .doitChangerMotDePasse(true)
                .motDePasse(passwordEncoder.encode(motDePasseTemporaire))
                .build();

        responsableEntrepriseRepository.save(responsable);
        try {
            notificationService.notifierCreationCompteEntreprise(responsable.getEmail(), motDePasseTemporaire, entreprise.getNom());
        } catch (Exception ex) {
            // La notification ne doit jamais faire échouer la création du compte.
            log.warn("Notification de creation de compte non envoyee pour {} : {}", responsable.getEmail(), ex.getMessage());
        }
        accountEmailService.sendAccountCreatedEmailAsync(
                responsable.getPrenom(),
                responsable.getEmail(),
                motDePasseTemporaire
        );
    }

    private Long resolveAuthorizedStagiaireId(Long requestedStagiaireId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.STAGIAIRE) {
            return utilisateur.getId();
        }

        if (requestedStagiaireId == null) {
            throw new IllegalArgumentException("Le stagiaire est obligatoire");
        }

        return requestedStagiaireId;
    }

    private void authorizeStagiaireAccess(Long stagiaireId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.STAGIAIRE && !utilisateur.getId().equals(stagiaireId)) {
            throw new AccessDeniedException("Acces refuse a des demandes qui ne vous appartiennent pas.");
        }
    }

    private void authorizeDemandeOwnerIfNeeded(DemandeCreationCompteEntreprise demande) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.STAGIAIRE) {
            Long ownerId = demande.getStagiaire() != null ? demande.getStagiaire().getId() : null;
            if (ownerId == null || !ownerId.equals(utilisateur.getId())) {
                throw new AccessDeniedException("Acces refuse a cette demande.");
            }
        }
    }

    private void ensureDemandeEditable(DemandeCreationCompteEntreprise demande) {
        if (demande.getStatut() == StatutDemande.VALIDEE) {
            throw new IllegalArgumentException("La demande est deja validee et l'entreprise a ete creee automatiquement.");
        }
        if (demande.getStatut() == StatutDemande.REFUSEE) {
            throw new IllegalArgumentException("Une demande refusee ne peut plus etre modifiee.");
        }
    }

    private void ensureDemandeStillActionable(DemandeCreationCompteEntreprise demande) {
        if (demande.getStatut() == StatutDemande.VALIDEE) {
            throw new IllegalArgumentException("La demande a deja ete completement validee et l'entreprise a ete creee automatiquement.");
        }
        if (demande.getStatut() == StatutDemande.REFUSEE) {
            throw new IllegalArgumentException("La demande a deja ete refusee.");
        }
    }

    private String normalizeRequiredComment(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new IllegalArgumentException("Veuillez saisir le motif du refus");
        }

        String normalized = commentaire.trim();
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("Le motif du refus ne doit pas depasser 1000 caracteres.");
        }
        return normalized;
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
    }

    private String genererMotDePasseTemporaire() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        final int length = 12;
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private NormalizedDemandeRequest normalizeAndValidateRequest(CreateDemandeCreationCompteEntrepriseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La demande est obligatoire.");
        }

        String nomEntreprise = requireText(request.getNomEntreprise(), "Le nom de l'entreprise est obligatoire.");
        String adresse = requireText(request.getAdresse(), "L'adresse de l'entreprise est obligatoire.");
        String secteurActivite = requireText(request.getSecteurActivite(), "Le secteur d'activite est obligatoire.");
        String nomResponsable = requireText(request.getNomResponsable(), "Le nom du responsable est obligatoire.");
        String prenomResponsable = requireText(request.getPrenomResponsable(), "Le prenom du responsable est obligatoire.");

        String emailEntreprise = contactUniquenessService.normalizeAndValidateRequiredEmail(
                request.getEmailEntreprise(),
                "emailEntreprise"
        );
        String telephoneEntreprise = contactUniquenessService.normalizeAndValidateRequiredPhone(
                request.getTelephoneEntreprise(),
                "telephoneEntreprise"
        );
        String emailResponsable = contactUniquenessService.normalizeAndValidateRequiredEmail(
                request.getEmailResponsable(),
                "emailResponsable"
        );
        String telephoneResponsable = contactUniquenessService.normalizeAndValidateRequiredPhone(
                request.getTelephoneResponsable(),
                "telephoneResponsable"
        );

        contactUniquenessService.validateEntrepriseContactForCreate(emailEntreprise, telephoneEntreprise);
        validateRepresentativeContactAvailability(emailResponsable, telephoneResponsable);

        return new NormalizedDemandeRequest(
                nomEntreprise,
                emailEntreprise,
                telephoneEntreprise,
                adresse,
                secteurActivite,
                nomResponsable,
                prenomResponsable,
                emailResponsable,
                telephoneResponsable
        );
    }

    private void validateRepresentativeContactAvailability(String emailResponsable, String telephoneResponsable) {
        utilisateurRepository.findByEmailIgnoreCase(emailResponsable).ifPresent(existing -> {
            throw new fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException(
                    "emailResponsable",
                    "L'email du representant est deja utilise."
            );
        });

        responsableEntrepriseRepository.findByEmailIgnoreCase(emailResponsable).ifPresent(existing -> {
            throw new fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException(
                    "emailResponsable",
                    "L'email du representant est deja utilise."
            );
        });

        utilisateurRepository.findByTelephone(telephoneResponsable).ifPresent(existing -> {
            throw new fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException(
                    "telephoneResponsable",
                    "Le numero du representant est deja utilise."
            );
        });

        responsableEntrepriseRepository.findByTelephone(telephoneResponsable).ifPresent(existing -> {
            throw new fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException(
                    "telephoneResponsable",
                    "Le numero du representant est deja utilise."
            );
        });
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record NormalizedDemandeRequest(
            String nomEntreprise,
            String emailEntreprise,
            String telephoneEntreprise,
            String adresse,
            String secteurActivite,
            String nomResponsable,
            String prenomResponsable,
            String emailResponsable,
            String telephoneResponsable
    ) {}
}
