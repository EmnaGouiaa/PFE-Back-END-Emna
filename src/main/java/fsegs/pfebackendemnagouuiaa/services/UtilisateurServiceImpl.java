package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailResponse;
import fsegs.pfebackendemnagouuiaa.dto.UpdatePasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateProfileRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.exception.AccountCreationException;
import fsegs.pfebackendemnagouuiaa.exception.AccountEmailDeliveryException;
import fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException;
import fsegs.pfebackendemnagouuiaa.mapper.UtilisateurMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.entities.Stage;

@Service
@Slf4j
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final String RESPONSABLE_ENTREPRISE_ONLY_CREATE_MESSAGE =
            "Le responsable entreprise peut seulement creer un encadrant professionnel.";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ContactUniquenessService contactUniquenessService;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;
    private final StageRepository stageRepository;
    private final ConventionStageRepository conventionStageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final NotificationService notificationService;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        validateAdminManagedRoleCreation(request.getRole());

        Utilisateur utilisateur = UtilisateurMapper.toEntity(request);
        String email;
        String telephone;
        String matricule;

        try {
            email = contactUniquenessService.normalizeAndValidateRequiredEmail(request.getEmail(), "email");
            telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(request.getTelephone(), "telephone");
            matricule = contactUniquenessService.normalizeAndValidateOptionalMatricule(request.getMatricule(), "matricule");
            contactUniquenessService.validateUserIdentityForCreate(email, telephone, matricule);
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Creation utilisateur refusee pour erreur de validation. email={}, role={}, reason={}",
                    safeValue(request.getEmail()),
                    request.getRole(),
                    ex.getMessage()
            );
            throw ex;
        } catch (DuplicateFieldException ex) {
            log.warn(
                    "Creation utilisateur refusee pour doublon. email={}, telephone={}, field={}, reason={}",
                    safeValue(request.getEmail()),
                    safeValue(request.getTelephone()),
                    ex.getField(),
                    ex.getMessage()
            );
            throw ex;
        }

        String generatedPassword = credentialPolicyService.generateStrongPassword();
        credentialPolicyService.validatePasswordStrength(generatedPassword);

        utilisateur.setEmail(email);
        utilisateur.setTelephone(telephone);
        utilisateur.setMatricule(matricule);
        utilisateur.setMotDePasse(passwordEncoder.encode(generatedPassword));
        utilisateur.setDoitChangerMotDePasse(true);

        Utilisateur saved;
        try {
            saved = utilisateurRepository.saveAndFlush(utilisateur);
            log.info(
                    "Compte utilisateur enregistre en base. id={}, email={}, role={}",
                    saved.getId(),
                    saved.getEmail(),
                    saved.getRole()
            );
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Creation utilisateur refusee par contrainte base. email={}, matricule={}, telephone={}, role={}",
                    email,
                    safeValue(matricule),
                    safeValue(telephone),
                    request.getRole(),
                    ex
            );
            throw resolveDuplicateFieldException(ex);
        } catch (RuntimeException ex) {
            log.error(
                    "Echec de sauvegarde du compte utilisateur. email={}, role={}",
                    email,
                    request.getRole(),
                    ex
            );
            throw new AccountCreationException("Echec de la creation du compte.", ex);
        }

        UserResponse response = UtilisateurMapper.toResponse(saved);

        try {
            accountEmailService.sendAccountCreatedEmail(saved.getPrenom(), saved.getEmail(), generatedPassword);
            response.setEmailSent(true);
            response.setMessage("Compte cree et email envoye avec succes.");
            log.info("Email de creation envoye pour le compte utilisateur. id={}, email={}", saved.getId(), saved.getEmail());
        } catch (AccountEmailDeliveryException ex) {
            response.setEmailSent(false);
            response.setMessage(buildSmtpFailureMessage("Compte cree, mais l'envoi de l'email a echoue.", ex));
            log.error(
                    "Echec SMTP apres creation du compte utilisateur. id={}, email={}",
                    saved.getId(),
                    saved.getEmail(),
                    ex
            );
        }

        return response;
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        String existingEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(utilisateur.getEmail(), "email");
        String email = contactUniquenessService.normalizeAndValidateRequiredEmail(request.getEmail(), "email");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(request.getTelephone(), "telephone");

        if (!existingEmail.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("L'email d'un utilisateur existant ne peut pas etre modifie.");
        }

        if (request.getRole() != utilisateur.getRole()) {
            throw new IllegalArgumentException("Le role d'un utilisateur existant ne peut pas etre modifie.");
        }

        contactUniquenessService.validateUserContactForUpdate(id, existingEmail, telephone);

        UtilisateurMapper.updateEntity(utilisateur, request);
        utilisateur.setEmail(existingEmail);
        utilisateur.setTelephone(telephone);
        Utilisateur updated = utilisateurRepository.save(utilisateur);
        return UtilisateurMapper.toResponse(updated);
    }

    @Override
    public UserResponse getCurrentProfile() {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
        return UtilisateurMapper.toProfileResponse(authenticatedUser);
    }

    @Override
    public UserResponse getProfile(Long id) {
        return UtilisateurMapper.toProfileResponse(findOwnedUser(id));
    }

    @Override
    @Transactional
    public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
        return updateProfile(authenticatedUser.getId(), request);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        Utilisateur utilisateur = findOwnedUser(id);
        String previousSignature = utilisateur.getNomFichierSignature();

        ensureEmailIsNotChangedByProfilePatch(utilisateur, request);
        ensureProfileRequestMatchesRole(utilisateur, request);

        if (request.getNom() != null) {
            utilisateur.setNom(requireNonBlank(request.getNom(), "Le nom ne peut pas etre vide"));
        }

        if (request.getPrenom() != null) {
            utilisateur.setPrenom(requireNonBlank(request.getPrenom(), "Le prenom ne peut pas etre vide"));
        }

        if (request.getTelephone() != null) {
            String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(request.getTelephone(), "telephone");
            if (telephone == null) {
                throw new IllegalArgumentException("Le numero de telephone ne peut pas etre vide");
            }
            contactUniquenessService.validateUserContactForUpdate(utilisateur.getId(), utilisateur.getEmail(), telephone);
            utilisateur.setTelephone(telephone);
        }

        if (request.getAdresse() != null) {
            utilisateur.setAdresse(requireNonBlank(request.getAdresse(), "L'adresse ne peut pas etre vide"));
        }

        if (request.getSpecialite() != null && utilisateur instanceof EncadrantAcademique encadrantAcademique) {
            encadrantAcademique.setSpecialite(requireNonBlank(
                    request.getSpecialite(),
                    "La specialite ne peut pas etre vide"
            ));
        }

        if (request.getGrade() != null && utilisateur instanceof EncadrantAcademique encadrantAcademique) {
            encadrantAcademique.setGrade(requireNonBlank(request.getGrade(), "Le grade ne peut pas etre vide"));
        }

        if (request.getDateNaiss() != null && utilisateur instanceof Stagiaire stagiaire) {
            stagiaire.setDateNaiss(request.getDateNaiss());
        }

        if (request.getMatricule() != null && utilisateur instanceof Stagiaire stagiaire) {
            String matricule = contactUniquenessService.normalizeAndValidateOptionalMatricule(request.getMatricule(), "matricule");
            if (matricule == null) {
                throw new IllegalArgumentException("Le matricule ne peut pas etre vide");
            }
            contactUniquenessService.validateUserIdentityForUpdate(
                    utilisateur.getId(),
                    utilisateur.getEmail(),
                    utilisateur.getTelephone(),
                    matricule
            );
            stagiaire.setMatricule(matricule);
        }

        if (request.getPoste() != null && utilisateur instanceof EncadrantProfessionnel encadrantProfessionnel) {
            encadrantProfessionnel.setPoste(requireNonBlank(request.getPoste(), "Le poste ne peut pas etre vide"));
        }

        if (request.getService() != null && utilisateur instanceof EncadrantProfessionnel encadrantProfessionnel) {
            encadrantProfessionnel.setService(requireNonBlank(request.getService(), "Le service ne peut pas etre vide"));
        }

        if (request.getPoste() != null && utilisateur instanceof ResponsableEntreprise responsableEntreprise) {
            responsableEntreprise.setPoste(requireNonBlank(request.getPoste(), "Le poste ne peut pas etre vide"));
        }

        if (request.getService() != null && utilisateur instanceof ResponsableEntreprise responsableEntreprise) {
            responsableEntreprise.setService(requireNonBlank(request.getService(), "Le service ne peut pas etre vide"));
        }

        if (request.getService() != null && utilisateur instanceof ResponsableServiceStages responsableServiceStages) {
            responsableServiceStages.setService(requireNonBlank(request.getService(), "Le service ne peut pas etre vide"));
        }

        if (request.getNomFichierSignature() != null) {
            utilisateur.setNomFichierSignature(requireNonBlank(
                    request.getNomFichierSignature(),
                    "Le nom du fichier de signature ne peut pas etre vide"
            ));
        }

        Utilisateur updated = utilisateurRepository.save(utilisateur);
        synchroniserDonneesStagiaireLiees(updated);
        notifierStagiairesSiSignatureAjouteeOuMiseAJour(updated, previousSignature, request.getNomFichierSignature() != null);
        return UtilisateurMapper.toProfileResponse(updated);
    }

    @Override
    @Transactional
    public UpdateEmailResponse updateCurrentEmail(UpdateEmailRequest request) {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        String newEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(request.getEmail(), "email");

        if (!passwordEncoder.matches(request.getMotDePasseActuel(), utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }

        Long utilisateurId = utilisateur.getId();
        boolean emailAlreadyUsed = utilisateurRepository.existsByEmailIgnoreCaseAndIdNot(newEmail, utilisateurId)
                || utilisateurRepository.findByNormalizedEmail(newEmail)
                .filter(existing -> !existing.getId().equals(utilisateurId))
                .isPresent();
        if (emailAlreadyUsed) {
            throw new IllegalArgumentException("Cette adresse e-mail est déjà utilisée");
        }

        String currentEmail = normalizeEmailValue(utilisateur.getEmail());
        if (!currentEmail.equals(newEmail)) {
            utilisateur.setEmail(newEmail);
            try {
                utilisateur = utilisateurRepository.saveAndFlush(utilisateur);
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalArgumentException("Cette adresse e-mail est déjà utilisée", ex);
            }
        }

        synchroniserDonneesStagiaireLiees(utilisateur);

        String token = jwtService.generateToken(utilisateur);

        return UpdateEmailResponse.builder()
                .message("Adresse e-mail modifiée avec succès")
                .token(token)
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateCurrentPassword(UpdatePasswordRequest request) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
        return updatePassword(authenticatedUser.getId(), request);
    }

    @Override
    @Transactional
    public UserResponse updatePassword(Long id, UpdatePasswordRequest request) {
        Utilisateur utilisateur = findOwnedUser(id);

        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
        }

        if (!request.getNouveauMotDePasse().equals(request.getConfirmationNouveauMotDePasse())) {
            throw new IllegalArgumentException("La confirmation du nouveau mot de passe ne correspond pas");
        }

        if (request.getAncienMotDePasse().equals(request.getNouveauMotDePasse())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit etre different de l'ancien");
        }

        credentialPolicyService.validatePasswordStrength(request.getNouveauMotDePasse());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setDoitChangerMotDePasse(false);
        Utilisateur updated = utilisateurRepository.save(utilisateur);
        return UtilisateurMapper.toResponse(updated);
    }

    @Override
    public UserResponse getUserById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findVisibleUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        return UtilisateurMapper.toResponse(utilisateur);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return utilisateurRepository.findVisibleUsers()
                .stream()
                .map(UtilisateurMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse deactivateUser(Long id) {
        return updateUserActivation(id, false);
    }

    @Override
    public UserResponse activateUser(Long id) {
        return updateUserActivation(id, true);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        if (Boolean.TRUE.equals(utilisateur.getSupprime())) {
            throw new EntityNotFoundException("Cet utilisateur a deja ete supprime.");
        }

        if (authenticatedUser.getId().equals(utilisateur.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte.");
        }

        utilisateur.setSupprime(true);
        utilisateur.setActif(false);
        utilisateur.setDoitChangerMotDePasse(false);
        utilisateur.setEmail(buildDeletedUniqueValue("deleted", utilisateur.getId(), utilisateur.getEmail()));
        utilisateur.setTelephone(buildDeletedUniqueValue("deleted-phone", utilisateur.getId(), utilisateur.getTelephone()));
        utilisateur.setMatricule(buildDeletedUniqueValue("deleted-matricule", utilisateur.getId(), utilisateur.getMatricule()));

        utilisateurRepository.save(utilisateur);
    }

    private UserResponse updateUserActivation(Long id, boolean active) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        Utilisateur utilisateur = utilisateurRepository.findVisibleUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        if (authenticatedUser.getRole() != null && authenticatedUser.getRole().name().equals("RESPONSABLE_ENTREPRISE")) {
            if (utilisateur instanceof EncadrantProfessionnel) {
                throw new AccessDeniedException(RESPONSABLE_ENTREPRISE_ONLY_CREATE_MESSAGE);
            }

            throw new AccessDeniedException("Seul l'administrateur peut modifier le statut d'un utilisateur.");
        }

        utilisateur.setActif(active);
        Utilisateur updated = utilisateurRepository.save(utilisateur);
        return UtilisateurMapper.toResponse(updated);
    }

    private void validateAdminManagedRoleCreation(Role role) {
        if (role == Role.RESPONSABLE_ENTREPRISE || role == Role.ENCADRANT_PROFESSIONNEL) {
            throw new IllegalArgumentException(
                    "Ce role doit etre cree depuis la section Entreprises & responsables."
            );
        }
    }

    private Utilisateur findOwnedUser(Long id) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        if (!authenticatedUser.getId().equals(id)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que votre propre profil.");
        }

        return utilisateurRepository.findById(id)
                .filter(utilisateur -> !Boolean.TRUE.equals(utilisateur.getSupprime()))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));
    }

    private void ensureProfileRequestMatchesRole(Utilisateur utilisateur, UpdateProfileRequest request) {
        boolean stagiaire = utilisateur instanceof Stagiaire;
        boolean encadrantAcademique = utilisateur instanceof EncadrantAcademique;
        boolean encadrantProfessionnel = utilisateur instanceof EncadrantProfessionnel;
        boolean responsableEntreprise = utilisateur instanceof ResponsableEntreprise;
        boolean responsableServiceStages = utilisateur instanceof ResponsableServiceStages;

        rejectIfUnsupported(request.getDateNaiss() != null && !stagiaire, "dateNaiss");
        rejectIfUnsupported(request.getMatricule() != null && !stagiaire, "matricule");
        rejectIfUnsupported(request.getNiveau() != null, "niveau");
        rejectIfUnsupported(request.getGrade() != null && !encadrantAcademique, "grade");
        rejectIfUnsupported(request.getSpecialite() != null && !encadrantAcademique, "specialite");
        rejectIfUnsupported(request.getPoste() != null && !(encadrantProfessionnel || responsableEntreprise), "poste");
        rejectIfUnsupported(
                request.getService() != null && !(encadrantProfessionnel || responsableEntreprise || responsableServiceStages),
                "service"
        );
    }

    private void rejectIfUnsupported(boolean unsupported, String fieldName) {
        if (unsupported) {
            throw new AccessDeniedException("Le champ " + fieldName + " n'est pas autorise pour votre role.");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "<vide>" : value.trim();
    }

    private void ensureEmailIsNotChangedByProfilePatch(Utilisateur utilisateur, UpdateProfileRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return;
        }

        String requestedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(request.getEmail(), "email");
        String currentEmail = normalizeEmailValue(utilisateur.getEmail());
        if (!currentEmail.equals(requestedEmail)) {
            throw new IllegalArgumentException(
                    "La modification de l'adresse e-mail doit etre confirmee avec le mot de passe actuel."
            );
        }
    }

    private String normalizeEmailValue(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildSmtpFailureMessage(String fallbackMessage, AccountEmailDeliveryException ex) {
        if (ex == null || ex.getDetails() == null || ex.getDetails().isBlank()) {
            return fallbackMessage;
        }
        return fallbackMessage + " " + ex.getDetails();
    }

    private DuplicateFieldException resolveDuplicateFieldException(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("email")) {
            return new DuplicateFieldException("email", "Cet email est deja utilise.");
        }
        if (normalizedMessage.contains("matricule")) {
            return new DuplicateFieldException("matricule", "Ce matricule est deja utilise.");
        }
        if (normalizedMessage.contains("telephone")) {
            return new DuplicateFieldException("telephone", "Ce numero de telephone est deja utilise.");
        }

        return new DuplicateFieldException(
                "utilisateur",
                "Utilisateur deja existant : email, matricule ou telephone deja utilise."
        );
    }

    private void notifierStagiairesSiSignatureAjouteeOuMiseAJour(Utilisateur utilisateur,
                                                                 String previousSignature,
                                                                 boolean signatureUpdated) {
        if (!signatureUpdated) {
            return;
        }

        String currentSignature = utilisateur.getNomFichierSignature();
        if (!hasText(currentSignature) || normalize(currentSignature).equals(normalize(previousSignature))) {
            return;
        }

        String action = hasText(previousSignature) ? "mise a jour" : "ajoutee";
        Map<Long, Long> stagiaireStageMap = new LinkedHashMap<>();

        for (Stage stage : findRelatedStages(utilisateur)) {
            Long stagiaireId = stage.getStagiaire() != null ? stage.getStagiaire().getId() : null;
            if (stagiaireId != null) {
                stagiaireStageMap.putIfAbsent(stagiaireId, stage.getId());
            }
        }

        for (Map.Entry<Long, Long> entry : stagiaireStageMap.entrySet()) {
            notificationService.creerNotification(
                    entry.getKey(),
                    "Signature disponible",
                    "Une signature a ete " + action + " pour votre dossier de stage.",
                    "SIGNATURE_DOSSIER_STAGE",
                    entry.getValue(),
                    "STAGE"
            );
        }
    }

    private List<Stage> findRelatedStages(Utilisateur utilisateur) {
        return switch (utilisateur.getRole()) {
            case ENCADRANT_ACADEMIQUE -> stageRepository.findByEncadrantAcademiqueId(utilisateur.getId());
            case ENCADRANT_PROFESSIONNEL -> stageRepository.findByEncadrantProfessionnelId(utilisateur.getId());
            case RESPONSABLE_ENTREPRISE -> stageRepository.findByTuteurEntrepriseId(utilisateur.getId());
            default -> List.of();
        };
    }

    private void synchroniserDonneesStagiaireLiees(Utilisateur utilisateur) {
        if (!(utilisateur instanceof Stagiaire stagiaire) || stagiaire.getId() == null) {
            return;
        }

        List<Stage> stages = stageRepository.findByStagiaireId(stagiaire.getId());
        if (stages.isEmpty()) {
            return;
        }

        String nomCompletStagiaire = buildFullName(stagiaire);
        for (Stage stage : stages) {
            if (stage == null || stage.getId() == null) {
                continue;
            }

            synchroniserConventionStagiaire(stage.getId(), stagiaire, nomCompletStagiaire);
            synchroniserCahierStageStagiaire(stage.getId(), stagiaire, nomCompletStagiaire);
        }
    }

    private void synchroniserConventionStagiaire(Long stageId, Stagiaire stagiaire, String nomCompletStagiaire) {
        conventionStageRepository.findByStageId(stageId)
                .ifPresent(convention -> {
                    boolean updated = false;

                    if (convention.getSignataireStagiaireId() != null
                            && convention.getSignataireStagiaireId().equals(stagiaire.getId())
                            && !sameText(convention.getNomSignataireStagiaire(), nomCompletStagiaire)) {
                        convention.setNomSignataireStagiaire(nomCompletStagiaire);
                        updated = true;
                    }

                    if (hasText(stagiaire.getNomFichierSignature())
                            && !sameText(convention.getImageSignatureStagiaire(), stagiaire.getNomFichierSignature())) {
                        convention.setImageSignatureStagiaire(stagiaire.getNomFichierSignature());
                        updated = true;
                    }

                    if (updated) {
                        conventionStageRepository.save(convention);
                    }
                });
    }

    private void synchroniserCahierStageStagiaire(Long stageId, Stagiaire stagiaire, String nomCompletStagiaire) {
        cahierStageRepository.findByStageId(stageId)
                .ifPresent(cahierStage -> {
                    boolean updated = false;

                    if (cahierStage.getSignataireStagiaireId() != null
                            && cahierStage.getSignataireStagiaireId().equals(stagiaire.getId())
                            && !sameText(cahierStage.getNomSignataireStagiaire(), nomCompletStagiaire)) {
                        cahierStage.setNomSignataireStagiaire(nomCompletStagiaire);
                        updated = true;
                    }

                    if (hasText(stagiaire.getNomFichierSignature())
                            && !sameText(cahierStage.getImageSignatureStagiaire(), stagiaire.getNomFichierSignature())) {
                        cahierStage.setImageSignatureStagiaire(stagiaire.getNomFichierSignature());
                        updated = true;
                    }

                    if (updated) {
                        cahierStageRepository.save(cahierStage);
                    }
                });
    }

    private String buildFullName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return "";
        }

        return ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom().trim()) + " "
                + (utilisateur.getNom() == null ? "" : utilisateur.getNom().trim())).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean sameText(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildDeletedUniqueValue(String prefix, Long id, String originalValue) {
        String original = normalize(originalValue);
        String suffix = hasText(original) ? "-" + original.replaceAll("\\s+", "-") : "";
        return prefix + "-" + id + suffix;
    }
}
