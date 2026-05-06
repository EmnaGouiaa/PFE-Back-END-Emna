package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.exception.AccountCreationException;
import fsegs.pfebackendemnagouuiaa.exception.AccountEmailDeliveryException;
import fsegs.pfebackendemnagouuiaa.exception.DuplicateFieldException;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
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
        NormalizedCompanyAccountRequest normalized = normalizeForCreate(request);
        String generatedPassword = generatePassword();

        PersistedCompanyAccount persisted;
        try {
            persisted = transactionTemplate().execute(status -> {
                Entreprise savedEntreprise = entrepriseRepository.saveAndFlush(Entreprise.builder()
                        .nom(normalized.nomEntreprise())
                        .email(normalized.emailEntreprise())
                        .telephone(normalized.telephoneEntreprise())
                        .adresse(normalized.adresse())
                        .secteurActivite(normalized.secteurActivite())
                        .build());

                ResponsableEntreprise savedRepresentant = responsableEntrepriseRepository.saveAndFlush(ResponsableEntreprise.builder()
                        .nom(normalized.nomResponsable())
                        .prenom(normalized.prenomResponsable())
                        .email(normalized.emailResponsable())
                        .telephone(normalized.telephoneResponsable())
                        .motDePasse(passwordEncoder.encode(generatedPassword))
                        .doitChangerMotDePasse(true)
                        .role(Role.RESPONSABLE_ENTREPRISE)
                        .actif(true)
                        .entreprise(savedEntreprise)
                        .build());

                return new PersistedCompanyAccount(savedEntreprise, savedRepresentant, generatedPassword, true);
            });
        } catch (RuntimeException ex) {
            log.error(
                    "Echec de sauvegarde du compte entreprise. entreprise={}, emailResponsable={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable()),
                    ex
            );
            throw new AccountCreationException("Echec de la creation du compte.", ex);
        }

        if (persisted == null) {
            log.error(
                    "Echec de creation du compte entreprise sans exception explicite. entreprise={}, emailResponsable={}",
                    safeValue(request.getNomEntreprise()),
                    safeValue(request.getEmailResponsable())
            );
            throw new AccountCreationException("Echec de la creation du compte.");
        }

        log.info(
                "Compte entreprise enregistre en base. entrepriseId={}, representantId={}, emailResponsable={}",
                persisted.entreprise().getId(),
                persisted.representant().getId(),
                persisted.representant().getEmail()
        );

        AdminCompanyAccountResponse response = toResponse(persisted.entreprise(), persisted.representant());
        applyEmailOutcome(
                response,
                persisted.representant(),
                persisted.generatedPassword(),
                "Compte cree et email envoye avec succes.",
                "Compte cree, mais l'envoi de l'email a echoue."
        );
        return response;
    }

    @Override
    public AdminCompanyAccountResponse update(Long entrepriseId, AdminCompanyAccountRequest request) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + entrepriseId));

        ResponsableEntreprise representant = resolveRepresentant(entrepriseId, request.getRepresentantId());
        NormalizedCompanyAccountRequest normalized = normalizeForUpdate(request, entrepriseId, representant != null ? representant.getId() : null);

        boolean createdRepresentative = representant == null;
        String generatedPassword = createdRepresentative ? generatePassword() : null;

        PersistedCompanyAccount persisted;
        try {
            final ResponsableEntreprise existingRepresentant = representant;
            persisted = transactionTemplate().execute(status -> {
                entreprise.setNom(normalized.nomEntreprise());
                entreprise.setEmail(normalized.emailEntreprise());
                entreprise.setTelephone(normalized.telephoneEntreprise());
                entreprise.setAdresse(normalized.adresse());
                entreprise.setSecteurActivite(normalized.secteurActivite());
                Entreprise updatedEntreprise = entrepriseRepository.saveAndFlush(entreprise);

                ResponsableEntreprise representantToSave = existingRepresentant;
                if (representantToSave == null) {
                    representantToSave = ResponsableEntreprise.builder()
                            .motDePasse(passwordEncoder.encode(generatedPassword))
                            .doitChangerMotDePasse(true)
                            .role(Role.RESPONSABLE_ENTREPRISE)
                            .actif(true)
                            .entreprise(updatedEntreprise)
                            .build();
                }

                representantToSave.setNom(normalized.nomResponsable());
                representantToSave.setPrenom(normalized.prenomResponsable());
                representantToSave.setEmail(normalized.emailResponsable());
                representantToSave.setTelephone(normalized.telephoneResponsable());
                representantToSave.setRole(Role.RESPONSABLE_ENTREPRISE);
                representantToSave.setEntreprise(updatedEntreprise);
                ResponsableEntreprise savedRepresentant = responsableEntrepriseRepository.saveAndFlush(representantToSave);

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

        if (persisted.createdRepresentative() && persisted.generatedPassword() != null) {
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

            return new NormalizedCompanyAccountRequest(
                    request.getNomEntreprise().trim(),
                    entrepriseEmail,
                    entrepriseTelephone,
                    trimToNull(request.getAdresse()),
                    trimToNull(request.getSecteurActivite()),
                    request.getNomResponsable().trim(),
                    request.getPrenomResponsable().trim(),
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

            return new NormalizedCompanyAccountRequest(
                    request.getNomEntreprise().trim(),
                    entrepriseEmail,
                    entrepriseTelephone,
                    trimToNull(request.getAdresse()),
                    trimToNull(request.getSecteurActivite()),
                    request.getNomResponsable().trim(),
                    request.getPrenomResponsable().trim(),
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
