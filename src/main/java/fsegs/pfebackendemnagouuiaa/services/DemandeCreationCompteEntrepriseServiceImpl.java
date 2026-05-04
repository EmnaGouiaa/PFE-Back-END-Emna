package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateDemandeCreationCompteEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.StatutDemande;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.DemandeCreationCompteEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandeCreationCompteEntrepriseServiceImpl implements DemandeCreationCompteEntrepriseService {

    private final DemandeCreationCompteEntrepriseRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public DemandeCreationCompteEntreprise createDemande(CreateDemandeCreationCompteEntrepriseRequest request) {
        Long stagiaireId = resolveAuthorizedStagiaireId(request.getStagiaireId());
        Utilisateur stagiaire = utilisateurRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));

        DemandeCreationCompteEntreprise demande = DemandeCreationCompteEntreprise.builder()
                .stagiaire(stagiaire)
                .nomEntreprise(request.getNomEntreprise())
                .emailEntreprise(request.getEmailEntreprise())
                .telephoneEntreprise(request.getTelephoneEntreprise())
                .adresse(request.getAdresse())
                .secteurActivite(request.getSecteurActivite())
                .nomResponsable(request.getNomResponsable())
                .prenomResponsable(request.getPrenomResponsable())
                .emailResponsable(request.getEmailResponsable())
                .build();

        return demandeRepository.save(demande);
    }

    @Override
    public DemandeCreationCompteEntreprise updateDemande(Long id, CreateDemandeCreationCompteEntrepriseRequest request) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        authorizeDemandeOwnerIfNeeded(demande);
        ensureDemandeEditable(demande);

        Long stagiaireId = resolveAuthorizedStagiaireId(request.getStagiaireId());
        Utilisateur stagiaire = utilisateurRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));

        demande.setStagiaire(stagiaire);
        demande.setNomEntreprise(request.getNomEntreprise());
        demande.setEmailEntreprise(request.getEmailEntreprise());
        demande.setTelephoneEntreprise(request.getTelephoneEntreprise());
        demande.setAdresse(request.getAdresse());
        demande.setSecteurActivite(request.getSecteurActivite());
        demande.setNomResponsable(request.getNomResponsable());
        demande.setPrenomResponsable(request.getPrenomResponsable());
        demande.setEmailResponsable(request.getEmailResponsable());

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
    public void deleteDemande(Long id) {
        if (!demandeRepository.existsById(id)) {
            throw new EntityNotFoundException("Demande introuvable");
        }
        demandeRepository.deleteById(id);
    }

    @Override
    public List<DemandeCreationCompteEntreprise> getDemandesByStagiaire(Long stagiaireId) {
        authorizeStagiaireAccess(stagiaireId);
        return demandeRepository.findByStagiaireId(stagiaireId);
    }

    @Override
    @Transactional
    public DemandeCreationCompteEntreprise validerParAdmin(Long demandeId, Long adminId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);
        authorizeAdminActor(adminId);

        utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin introuvable"));

        demande.setValideeParAdminId(adminId);
        demande.setStatutAdmin(StatutValidation.VALIDEE);
        mettreAJourStatutGlobal(demande);

        DemandeCreationCompteEntreprise saved = demandeRepository.save(demande);
        finaliserCreationCompteEntrepriseSiDoubleValidation(saved);
        return saved;
    }

    @Override
    public DemandeCreationCompteEntreprise refuserParAdmin(Long demandeId, Long adminId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);
        authorizeAdminActor(adminId);

        utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin introuvable"));

        demande.setValideeParAdminId(adminId);
        demande.setStatutAdmin(StatutValidation.REFUSEE);
        demande.setStatut(StatutDemande.REFUSEE);

        return demandeRepository.save(demande);
    }

    @Override
    public DemandeCreationCompteEntreprise validerParEncadrantAcademique(Long demandeId, Long encadrantId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);

        Utilisateur encadrant = utilisateurRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant introuvable"));

        demande.setValideeParEncadrantAcademique(encadrant);
        demande.setValideeParEncadrantId(encadrantId);
        mettreAJourStatutGlobal(demande);

        return demandeRepository.save(demande);
    }

    @Override
    public DemandeCreationCompteEntreprise refuserParEncadrantAcademique(Long demandeId, Long encadrantId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);

        Utilisateur encadrant = utilisateurRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant introuvable"));

        demande.setValideeParEncadrantAcademique(encadrant);
        demande.setValideeParEncadrantId(encadrantId);
        demande.setStatut(StatutDemande.REFUSEE);

        return demandeRepository.save(demande);
    }

    @Override
    @Transactional
    public DemandeCreationCompteEntreprise validerParResponsableStages(Long demandeId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);

        demande.setStatutResponsableStages(StatutValidation.VALIDEE);
        mettreAJourStatutGlobal(demande);

        DemandeCreationCompteEntreprise saved = demandeRepository.save(demande);
        finaliserCreationCompteEntrepriseSiDoubleValidation(saved);
        return saved;
    }

    @Override
    public DemandeCreationCompteEntreprise refuserParResponsableStages(Long demandeId) {
        DemandeCreationCompteEntreprise demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        ensureDemandeStillActionable(demande);

        demande.setStatutResponsableStages(StatutValidation.REFUSEE);
        demande.setStatut(StatutDemande.REFUSEE);

        return demandeRepository.save(demande);
    }

    private void mettreAJourStatutGlobal(DemandeCreationCompteEntreprise demande) {
        boolean adminValide = demande.getStatutAdmin() == StatutValidation.VALIDEE;
        boolean responsableValide = demande.getStatutResponsableStages() == StatutValidation.VALIDEE;
        boolean adminRefuse = demande.getStatutAdmin() == StatutValidation.REFUSEE;
        boolean responsableRefuse = demande.getStatutResponsableStages() == StatutValidation.REFUSEE;

        if (adminValide && responsableValide) {
            demande.setStatut(StatutDemande.VALIDEE);
        } else if (adminRefuse || responsableRefuse) {
            demande.setStatut(StatutDemande.REFUSEE);
        } else {
            demande.setStatut(StatutDemande.EN_ATTENTE);
        }
    }

    private void finaliserCreationCompteEntrepriseSiDoubleValidation(DemandeCreationCompteEntreprise demande) {
        if (demande == null || demande.getId() == null) return;
        if (demande.getStatut() != StatutDemande.VALIDEE) return;

        creerEntrepriseDepuisDemandeSiNecessaire(demande);
        Long stagiaireId = demande.getStagiaire() != null ? demande.getStagiaire().getId() : null;
        if (stagiaireId != null) {
            notificationService.notifierDemandeEntrepriseValidee(stagiaireId, demande.getId(), demande.getNomEntreprise());
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
                .telephone(demande.getTelephoneEntreprise())
                .entreprise(entreprise)
                .role(Role.RESPONSABLE_ENTREPRISE)
                .actif(true)
                .motDePasse(passwordEncoder.encode(motDePasseTemporaire))
                .build();

        responsableEntrepriseRepository.save(responsable);
        notificationService.notifierCreationCompteEntreprise(responsable.getEmail(), motDePasseTemporaire, entreprise.getNom());
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

    private void authorizeAdminActor(Long adminId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.ADMINISTRATEUR && !utilisateur.getId().equals(adminId)) {
            throw new AccessDeniedException("L'identifiant administrateur ne correspond pas a la session authentifiee.");
        }
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
}
