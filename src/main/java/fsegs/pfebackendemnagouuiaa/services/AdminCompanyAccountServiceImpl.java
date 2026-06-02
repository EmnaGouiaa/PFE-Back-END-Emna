package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;
import fsegs.pfebackendemnagouuiaa.validation.PersonNameValidation;
import fsegs.pfebackendemnagouuiaa.dto.CreateRepresentantEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.dto.RepresentantEntrepriseResponse;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.exception.AccountCreationException;
import fsegs.pfebackendemnagouuiaa.exception.AccountEmailDeliveryException;
import fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.services.EntrepriseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminCompanyAccountServiceImpl implements AdminCompanyAccountService {

    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final ContactUniquenessService contactUniquenessService;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final EntrepriseService entrepriseService;

    @Override
    public List<AdminCompanyAccountResponse> getAll() {
        return entrepriseRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Entreprise::getId).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminCompanyAccountResponse create(AdminCompanyAccountRequest request) {
        // Le formulaire "+ Nouvelle fiche" cree l'entreprise ET son representant en une
        // seule soumission. On valide d'abord les donnees (unicite email/telephone), puis
        // dans une meme transaction : (1) creation de l'entreprise, (2) recuperation de son
        // id genere, (3) creation du representant rattache a cette entreprise.
        NormalizedCompanyAccountRequest normalized = normalizeForCreate(request);
        final String generatedPassword = generatePassword();

        PersistedCompanyAccount persisted;
        try {
            persisted = transactionTemplate().execute(status -> {
                // 1. Creer d'abord l'entreprise.
                Entreprise entreprise = Entreprise.builder()
                        .nom(normalized.nomEntreprise())
                        .email(normalized.emailEntreprise())
                        .telephone(normalized.telephoneEntreprise())
                        .adresse(normalized.adresse())
                        .secteurActivite(normalized.secteurActivite())
                        .build();
                // 2. saveAndFlush garantit que l'id de l'entreprise est genere et disponible.
                Entreprise savedEntreprise = entrepriseRepository.saveAndFlush(entreprise);

                // 3. Creer ensuite le representant lie a l'entreprise tout juste creee.
                ResponsableEntreprise representant = ResponsableEntreprise.builder()
                        .nom(normalized.nomResponsable())
                        .prenom(normalized.prenomResponsable())
                        .email(normalized.emailResponsable())
                        .telephone(normalized.telephoneResponsable())
                        .motDePasse(passwordEncoder.encode(generatedPassword))
                        .doitChangerMotDePasse(true)
                        .role(Role.RESPONSABLE_ENTREPRISE)
                        .actif(true)
                        .supprime(false)
                        .entreprise(savedEntreprise)
                        .build();
                ResponsableEntreprise savedRepresentant = responsableEntrepriseRepository.saveAndFlush(representant);

                return new PersistedCompanyAccount(savedEntreprise, savedRepresentant, generatedPassword, true);
            });
        } catch (DuplicateFieldException | IllegalArgumentException | EntityNotFoundException ex) {
            // Erreurs metier/validation : on les laisse remonter telles quelles.
            throw ex;
        } catch (RuntimeException ex) {
            log.error(
                    "Echec de creation de l'entreprise ou du representant. entreprise={}, emailResponsable={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable()),
                    ex
            );
            throw new AccountCreationException("Echec de la creation du compte.", ex);
        }

        if (persisted == null || persisted.representant() == null) {
            log.error(
                    "Echec de creation entreprise/representant sans exception explicite. entreprise={}, emailResponsable={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable())
            );
            throw new AccountCreationException("Echec de la creation du compte.");
        }

        AdminCompanyAccountResponse response = toResponse(persisted.entreprise(), persisted.representant());
        applyEmailOutcome(
                response,
                persisted.representant(),
                persisted.generatedPassword(),
                "Entreprise et representant crees avec succes. Les identifiants ont ete envoyes par email.",
                "Entreprise et representant crees, mais l'envoi de l'email a echoue."
        );
        return response;
    }

    @Override
    public AdminCompanyAccountResponse update(Long entrepriseId, AdminCompanyAccountRequest request) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + entrepriseId));

        ResponsableEntreprise representant = resolveRepresentant(entrepriseId, request.getRepresentantId());
        contactUniquenessService.assertEntrepriseEmailUnchanged(entreprise.getEmail(), request.getEmailEntreprise());
        if (representant != null) {
            contactUniquenessService.assertResponsableEntrepriseEmailUnchanged(
                    representant.getEmail(),
                    request.getEmailResponsable()
            );
        }
        NormalizedCompanyAccountRequest normalized = normalizeForUpdate(request, entrepriseId, representant != null ? representant.getId() : null);

        boolean createdRepresentative = false;
        String generatedPassword = null;

        PersistedCompanyAccount persisted;
        try {
            final ResponsableEntreprise existingRepresentant = representant;
            persisted = transactionTemplate().execute(status -> {
                entreprise.setNom(normalized.nomEntreprise());
                entreprise.setTelephone(normalized.telephoneEntreprise());
                entreprise.setAdresse(normalized.adresse());
                entreprise.setSecteurActivite(normalized.secteurActivite());
                Entreprise updatedEntreprise = entrepriseRepository.saveAndFlush(entreprise);

                ResponsableEntreprise savedRepresentant = null;
                if (existingRepresentant != null) {
                    existingRepresentant.setNom(normalized.nomResponsable());
                    existingRepresentant.setPrenom(normalized.prenomResponsable());
                    existingRepresentant.setTelephone(normalized.telephoneResponsable());
                    existingRepresentant.setRole(Role.RESPONSABLE_ENTREPRISE);
                    existingRepresentant.setEntreprise(updatedEntreprise);
                    savedRepresentant = responsableEntrepriseRepository.saveAndFlush(existingRepresentant);
                }

                return new PersistedCompanyAccount(updatedEntreprise, savedRepresentant, generatedPassword, createdRepresentative);
            });
        } catch (RuntimeException ex) {
            log.error(
                    "Echec de sauvegarde de l'entreprise ou du representant. entrepriseId={}, emailResponsable={}",
                    entrepriseId,
                    safeValue(request.getEmailResponsable()),
                    ex
            );
            throw new AccountCreationException("Echec de la creation du compte.", ex);
        }

        if (persisted == null) {
            log.error(
                    "Echec de mise a jour entreprise sans exception explicite. entrepriseId={}, emailResponsable={}",
                    entrepriseId,
                    safeValue(request.getEmailResponsable())
            );
            throw new AccountCreationException("Echec de la creation du compte.");
        }

        AdminCompanyAccountResponse response = toResponse(persisted.entreprise(), persisted.representant());

        if (persisted.createdRepresentative() && persisted.representant() != null && persisted.generatedPassword() != null) {
            applyEmailOutcome(
                    response,
                    persisted.representant(),
                    persisted.generatedPassword(),
                    "Entreprise et representant mis a jour avec succes. Les identifiants ont ete envoyes par email.",
                    "Entreprise et representant mis a jour, mais l'envoi de l'email a echoue."
            );
        }

        return response;
    }

    @Override
    public List<EntrepriseDto> getAllEntreprisesForAdmin() {
        return entrepriseService.getAll();
    }

    @Override
    public RepresentantEntrepriseResponse createRepresentantEntreprise(CreateRepresentantEntrepriseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requéte de création du représentant est obligatoire.");
        }

        String nom = requireText(request.getNom(), "Le nom est obligatoire.");
        String prenom = requireText(request.getPrenom(), "Le prénom est obligatoire.");
        String email = contactUniquenessService.normalizeAndValidateRequiredEmail(request.getEmail(), "email");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(request.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForCreate(email, telephone);
        credentialPolicyService.validatePasswordStrength(request.getMotDePasse());

        if (request.getEntrepriseId() == null) {
            throw new IllegalArgumentException("L'entreprise existante est obligatoire pour creer un representant.");
        }

        Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + request.getEntrepriseId()));

        ResponsableEntreprise representant = ResponsableEntreprise.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .telephone(telephone)
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .doitChangerMotDePasse(false)
                .role(Role.RESPONSABLE_ENTREPRISE)
                .actif(true)
                .entreprise(entreprise)
                .build();

        ResponsableEntreprise saved = responsableEntrepriseRepository.save(representant);
        return toRepresentantResponse(saved);
    }

    private ResponsableEntreprise resolveRepresentant(Long entrepriseId, Long representantId) {
        if (representantId != null) {
            return responsableEntrepriseRepository.findById(representantId)
                    .orElseThrow(() -> new EntityNotFoundException("Representant introuvable avec l'id : " + representantId));
        }

        return responsableEntrepriseRepository.findByEntrepriseId(entrepriseId)
                .stream()
                .min(Comparator.comparing(ResponsableEntreprise::getId))
                .orElse(null);
    }

    private AdminCompanyAccountResponse toResponse(Entreprise entreprise) {
        ResponsableEntreprise representant = responsableEntrepriseRepository.findByEntrepriseId(entreprise.getId())
                .stream()
                .min(Comparator.comparing(ResponsableEntreprise::getId))
                .orElse(null);
        return toResponse(entreprise, representant);
    }

    private AdminCompanyAccountResponse toResponse(Entreprise entreprise, ResponsableEntreprise representant) {
        return AdminCompanyAccountResponse.builder()
                .entrepriseId(entreprise.getId())
                .nomEntreprise(entreprise.getNom())
                .emailEntreprise(entreprise.getEmail())
                .telephoneEntreprise(entreprise.getTelephone())
                .adresse(entreprise.getAdresse())
                .secteurActivite(entreprise.getSecteurActivite())
                .representantId(representant != null ? representant.getId() : null)
                .nomResponsable(representant != null ? representant.getNom() : null)
                .prenomResponsable(representant != null ? representant.getPrenom() : null)
                .emailResponsable(representant != null ? representant.getEmail() : null)
                .telephoneResponsable(representant != null ? representant.getTelephone() : null)
                .build();
    }

    private NormalizedCompanyAccountRequest normalizeForCreate(AdminCompanyAccountRequest request) {
        try {
            String entrepriseEmail = contactUniquenessService.normalizeAndValidateOptionalEmail(
                    request.getEmailEntreprise(),
                    "emailEntreprise"
            );
            String entrepriseTelephone = contactUniquenessService.normalizeAndValidateOptionalPhone(
                    request.getTelephoneEntreprise(),
                    "telephoneEntreprise"
            );
            String responsableEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(
                    request.getEmailResponsable(),
                    "emailResponsable"
            );
            String responsableTelephone = contactUniquenessService.normalizeAndValidateOptionalPhone(
                    request.getTelephoneResponsable(),
                    "telephoneResponsable"
            );

            contactUniquenessService.validateEntrepriseContactForCreate(entrepriseEmail, entrepriseTelephone);
            contactUniquenessService.validateUserContactForCreate(responsableEmail, responsableTelephone);

            String nomResponsable = request.getNomResponsable().trim();
            String prenomResponsable = request.getPrenomResponsable().trim();
            PersonNameValidation.requireValid(nomResponsable, "Le nom du responsable");
            PersonNameValidation.requireValid(prenomResponsable, "Le prenom du responsable");

            return new NormalizedCompanyAccountRequest(
                    request.getNomEntreprise().trim(),
                    entrepriseEmail,
                    entrepriseTelephone,
                    trimToNull(request.getAdresse()),
                    trimToNull(request.getSecteurActivite()),
                    nomResponsable,
                    prenomResponsable,
                    responsableEmail,
                    responsableTelephone
            );
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Creation compte entreprise refusee pour erreur de validation. entreprise={}, emailResponsable={}, reason={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable()),
                    ex.getMessage()
            );
            throw ex;
        } catch (DuplicateFieldException ex) {
            log.warn(
                    "Creation compte entreprise refusee pour doublon. entreprise={}, emailResponsable={}, field={}, reason={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable()),
                    ex.getField(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private NormalizedCompanyAccountRequest normalizeForUpdate(
            AdminCompanyAccountRequest request,
            Long entrepriseId,
            Long representantId
    ) {
        try {
            String entrepriseEmail = contactUniquenessService.normalizeAndValidateOptionalEmail(
                    request.getEmailEntreprise(),
                    "emailEntreprise"
            );
            String entrepriseTelephone = contactUniquenessService.normalizeAndValidateOptionalPhone(
                    request.getTelephoneEntreprise(),
                    "telephoneEntreprise"
            );
            String responsableEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(
                    request.getEmailResponsable(),
                    "emailResponsable"
            );
            String responsableTelephone = contactUniquenessService.normalizeAndValidateOptionalPhone(
                    request.getTelephoneResponsable(),
                    "telephoneResponsable"
            );

            contactUniquenessService.validateEntrepriseContactForUpdate(entrepriseId, entrepriseEmail, entrepriseTelephone);
            contactUniquenessService.validateUserContactForUpdate(representantId, responsableEmail, responsableTelephone);

            String nomResponsable = request.getNomResponsable().trim();
            String prenomResponsable = request.getPrenomResponsable().trim();
            PersonNameValidation.requireValid(nomResponsable, "Le nom du responsable");
            PersonNameValidation.requireValid(prenomResponsable, "Le prenom du responsable");

            return new NormalizedCompanyAccountRequest(
                    request.getNomEntreprise().trim(),
                    entrepriseEmail,
                    entrepriseTelephone,
                    trimToNull(request.getAdresse()),
                    trimToNull(request.getSecteurActivite()),
                    nomResponsable,
                    prenomResponsable,
                    responsableEmail,
                    responsableTelephone
            );
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Mise a jour compte entreprise refusee pour erreur de validation. entrepriseId={}, emailResponsable={}, reason={}",
                    entrepriseId,
                    safeValue(request.getEmailResponsable()),
                    ex.getMessage()
            );
            throw ex;
        } catch (DuplicateFieldException ex) {
            log.warn(
                    "Mise a jour compte entreprise refusee pour doublon. entrepriseId={}, emailResponsable={}, field={}, reason={}",
                    entrepriseId,
                    safeValue(request.getEmailResponsable()),
                    ex.getField(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private void applyEmailOutcome(
            AdminCompanyAccountResponse response,
            ResponsableEntreprise representant,
            String generatedPassword,
            String successMessage,
            String smtpFailureMessage
    ) {
        try {
            accountEmailService.sendAccountCreatedEmail(
                    representant.getPrenom(),
                    representant.getEmail(),
                    generatedPassword
            );
            response.setEmailSent(true);
            response.setMessage(successMessage);
            log.info(
                    "Email de creation envoye pour le representant entreprise. representantId={}, email={}",
                    representant.getId(),
                    representant.getEmail()
            );
        } catch (AccountEmailDeliveryException ex) {
            response.setEmailSent(false);
            response.setMessage(buildSmtpFailureMessage(smtpFailureMessage, ex));
            log.error(
                    "Echec SMTP apres sauvegarde du representant entreprise. representantId={}, email={}",
                    representant.getId(),
                    representant.getEmail(),
                    ex
            );
        }
    }

    private String buildSmtpFailureMessage(String fallbackMessage, AccountEmailDeliveryException ex) {
        if (ex == null || ex.getDetails() == null || ex.getDetails().isBlank()) {
            return fallbackMessage;
        }
        return fallbackMessage + " " + ex.getDetails();
    }

    private String generatePassword() {
        String generatedPassword = credentialPolicyService.generateStrongPassword();
        credentialPolicyService.validatePasswordStrength(generatedPassword);
        return generatedPassword;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private Entreprise resolveOrCreateEntreprise(CreateRepresentantEntrepriseRequest request) {
        if (request.getEntrepriseId() != null) {
            return entrepriseRepository.findById(request.getEntrepriseId())
                    .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + request.getEntrepriseId()));
        }

        String nomEntreprise = trimToNull(request.getNomEntreprise());
        String emailEntreprise = contactUniquenessService.normalizeAndValidateOptionalEmail(
                request.getEmailEntreprise(),
                "emailEntreprise"
        );
        String telephoneEntreprise = contactUniquenessService.normalizeAndValidateOptionalPhone(
                request.getTelephoneEntreprise(),
                "telephoneEntreprise"
        );

        if (nomEntreprise == null) {
            throw new IllegalArgumentException("L'entreprise est obligatoire soit par entrepriseId soit par nomEntreprise.");
        }

        Entreprise existingByEmail = emailEntreprise == null
                ? null
                : entrepriseRepository.findByEmailIgnoreCase(emailEntreprise).orElse(null);
        Entreprise existingByNom = entrepriseRepository.findByNomIgnoreCase(nomEntreprise).orElse(null);

        if (existingByEmail != null && existingByNom != null && !existingByEmail.getId().equals(existingByNom.getId())) {
            throw new DuplicateFieldException("entreprise", "Une incohérence a été détectée entre le nom et l'email de l'entreprise.");
        }

        Entreprise existing = existingByEmail != null ? existingByEmail : existingByNom;
        if (existing != null) {
            return existing;
        }

        contactUniquenessService.validateEntrepriseContactForCreate(emailEntreprise, telephoneEntreprise);

        Entreprise entreprise = Entreprise.builder()
                .nom(nomEntreprise)
                .adresse(trimToNull(request.getAdresseEntreprise()))
                .secteurActivite(trimToNull(request.getSecteurActivite()))
                .email(emailEntreprise)
                .telephone(telephoneEntreprise)
                .build();
        return entrepriseRepository.save(entreprise);
    }

    private RepresentantEntrepriseResponse toRepresentantResponse(ResponsableEntreprise representant) {
        return new RepresentantEntrepriseResponse(
                representant.getId(),
                representant.getNom(),
                representant.getPrenom(),
                representant.getEmail(),
                representant.getTelephone(),
                representant.getEntreprise() != null ? representant.getEntreprise().getId() : null,
                representant.getEntreprise() != null ? representant.getEntreprise().getNom() : null
        );
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "<vide>" : value.trim();
    }

    private record NormalizedCompanyAccountRequest(
            String nomEntreprise,
            String emailEntreprise,
            String telephoneEntreprise,
            String adresse,
            String secteurActivite,
            String nomResponsable,
            String prenomResponsable,
            String emailResponsable,
            String telephoneResponsable
    ) {
    }

    private record PersistedCompanyAccount(
            Entreprise entreprise,
            ResponsableEntreprise representant,
            String generatedPassword,
            boolean createdRepresentative
    ) {
    }
}
