package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.MailTestResponse;
import fsegs.pfebackendemnagouuiaa.exception.AccountEmailDeliveryException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEmailService {
    private static final DateTimeFormatter RESET_EXPIRATION_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.frontend.url:http://localhost:4200/connexion}")
    private String applicationLink;

    @Value("${spring.mail.username:no-reply@pfe.local}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:0}")
    private int smtpPort;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuthEnabled;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean startTlsEnabled;

    @PostConstruct
    void logMailConfigurationStatus() {
        logResolvedSmtpConfiguration("Demarrage");
        log.info(
                "Configuration SMTP chargee. host={}, port={}, username={}, auth={}, starttls={}",
                safeValue(smtpHost),
                smtpPort,
                maskEmail(fromAddress),
                smtpAuthEnabled,
                startTlsEnabled
        );

        if (!isMailConfigurationUsable()) {
            log.warn(
                "Configuration SMTP incomplete ou invalide. host={}, port={}, usernamePresent={}, passwordPresent={}",
                safeValue(smtpHost),
                smtpPort,
                hasText(fromAddress),
                hasText(getNormalizedSmtpPassword())
            );
        }
    }

    public void sendAccountCreatedEmail(String firstName, String email, String generatedPassword) {
        sendEmail(
                email,
                "Création de votre compte - Plateforme de gestion des stages",
                buildMessage(
                        firstName,
                        email,
                        generatedPassword,
                        "Votre compte a été créé sur la plateforme de gestion des stages.",
                        "Merci de vous connecter dès réception de cet e-mail et de changer votre mot de passe après votre première connexion."
                )
        );
    }

    public void sendProfessionalSupervisorAccountCreatedEmail(String firstName, String email, String generatedPassword) {
        sendEmail(
                email,
                "Bienvenue - Votre accès encadrant professionnel",
                buildMessage(
                        firstName,
                        email,
                        generatedPassword,
                        "Votre compte Encadrant Professionnel a été créé afin de vous permettre de suivre les stages de votre entreprise.",
                        "Nous vous invitons à vous connecter rapidement, à vérifier vos informations et à changer votre mot de passe après votre première connexion."
                )
        );
    }

    public void sendPasswordResetCodeEmail(String firstName, String email, String code, LocalDateTime expirationDate) {
        String recipient = firstName == null || firstName.isBlank() ? "Bonjour" : "Bonjour " + firstName;
        String body = recipient + ",\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe sur la plateforme de gestion des stages.\n\n"
                + "Votre code de vérification est : " + code + "\n"
                + "Ce code expire le : " + RESET_EXPIRATION_FORMAT.format(expirationDate) + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet e-mail.\n\n"
                + "Cordialement,\n"
                + "Administration - Plateforme de gestion des stages";

        sendEmail(
                email,
                "Code de reinitialisation de mot de passe - Plateforme de gestion des stages",
                body
        );
    }

    public void testSmtpConnection() {
        validateMailConfiguration();
        logResolvedSmtpConfiguration("Test SMTP");

        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            try {
                javaMailSender.setPassword(getNormalizedSmtpPassword());
                javaMailSender.testConnection();
                log.info(
                        "Connexion SMTP validee avec succes. host={}, port={}, username={}",
                        safeValue(smtpHost),
                        smtpPort,
                        maskEmail(fromAddress)
                );
                return;
            } catch (MessagingException ex) {
                throw buildDeliveryException(
                        "Validation de la connexion SMTP impossible.",
                        null,
                        ex
                );
            }
        }

        log.warn("Le bean JavaMailSender n'est pas une instance de JavaMailSenderImpl; testConnection() indisponible.");
    }

    public void sendTestEmail(String recipientEmail) {
        String normalizedRecipient = normalizeRecipient(recipientEmail);
        testSmtpConnection();
        sendEmail(
                normalizedRecipient,
                "Test SMTP - Plateforme de gestion des stages",
                "Ceci est un e-mail de test.\n\n"
                        + "SMTP authentifié avec succès pour : " + maskEmail(fromAddress) + "\n"
                        + "Destinataire : " + normalizedRecipient + "\n"
                        + "Lien de l'application : " + applicationLink + "\n"
                        + "Si vous recevez cet e-mail, la configuration SMTP est fonctionnelle.",
                true
        );
    }

    public MailTestResponse testSmtpAndSend(String recipientEmail) {
        String normalizedRecipient = normalizeRecipient(recipientEmail);
        testSmtpConnection();
        sendTestEmail(normalizedRecipient);
        return MailTestResponse.builder()
                .authenticationOk(true)
                .emailSent(true)
                .host(safeValue(smtpHost))
                .port(smtpPort)
                .username(maskEmail(resolveFromAddress()))
                .recipientEmail(normalizedRecipient)
                .message("Authentification SMTP validee et email de test envoye avec succes.")
                .build();
    }

    private void sendEmail(String email, String subject, String body) {
        sendEmail(email, subject, body, false);
    }

    private void sendEmail(String email, String subject, String body, boolean testEmail) {
        String normalizedRecipient = normalizeRecipient(email);
        String normalizedSubject = normalizeSubject(subject);
        String normalizedBody = normalizeBody(body);
        validateMailConfiguration();
        applyNormalizedPasswordToSender();

        try {
            sendMimeEmail(normalizedRecipient, normalizedSubject, normalizedBody, testEmail);
        } catch (MailException | MessagingException primaryException) {
            log.warn(
                    "Premier essai d'envoi email echoue. recipient={}, mode=MIME, diagnostic={}",
                    normalizedRecipient,
                    diagnoseFailure(primaryException)
            );

            try {
                sendSimpleTextEmail(normalizedRecipient, normalizedSubject, normalizedBody, testEmail);
            } catch (MailException fallbackException) {
                fallbackException.addSuppressed(primaryException);
                throw buildDeliveryException(
                        "Le compte a ete cree, mais l'e-mail n'a pas pu etre envoye.",
                        normalizedRecipient,
                        fallbackException
                );
            }
        }
    }

    private void sendMimeEmail(String recipient, String subject, String body, boolean testEmail)
            throws MessagingException, MailException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setFrom(resolveFromAddress());
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(body, false);

        log.info(
                "Tentative d'envoi email {}. recipient={}, from={}, subject={}, bodyLength={}, mode=MIME",
                testEmail ? "de test" : "applicatif",
                recipient,
                maskEmail(resolveFromAddress()),
                subject,
                body.length()
        );
        mailSender.send(mimeMessage);
        log.info(
                "Email {} envoye avec succes. recipient={}, from={}, mode=MIME",
                testEmail ? "de test" : "applicatif",
                recipient,
                maskEmail(resolveFromAddress())
        );
    }

    private void sendSimpleTextEmail(String recipient, String subject, String body, boolean testEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        log.info(
                "Nouvel essai d'envoi email {}. recipient={}, from={}, subject={}, bodyLength={}, mode=SimpleMailMessage",
                testEmail ? "de test" : "applicatif",
                recipient,
                maskEmail(resolveFromAddress()),
                subject,
                body.length()
        );
        mailSender.send(message);
        log.info(
                "Email {} envoye avec succes. recipient={}, from={}, mode=SimpleMailMessage",
                testEmail ? "de test" : "applicatif",
                recipient,
                maskEmail(resolveFromAddress())
        );
    }

    private String buildMessage(
            String firstName,
            String email,
            String generatedPassword,
            String welcomeLine,
            String followUpLine
    ) {
        String recipient = firstName == null || firstName.isBlank() ? "Bonjour" : "Bonjour " + firstName;
        return recipient + ",\n\n"
                + welcomeLine + "\n\n"
                + "Voici vos identifiants de connexion :\n"
                + "Email : " + email + "\n"
                + "Mot de passe temporaire : " + generatedPassword + "\n"
                + "Lien de l'application : " + applicationLink + "\n\n"
                + followUpLine + "\n\n"
                + "Cordialement,\n"
                + "Administration - Plateforme de gestion des stages";
    }

    private void validateMailConfiguration() {
        if (!isMailConfigurationUsable()) {
            String details = "Configuration SMTP invalide: host="
                    + safeValue(smtpHost)
                    + ", port=" + smtpPort
                    + ", usernamePresent=" + hasText(fromAddress)
                    + ", passwordPresent=" + hasText(getNormalizedSmtpPassword());
            log.error(details);
            throw new AccountEmailDeliveryException(
                    "La configuration SMTP est incomplete ou invalide.",
                    details,
                    null
            );
        }
        if (isPlaceholderPassword(getNormalizedSmtpPassword())) {
            String details = "Mot de passe SMTP invalide: remplacez le placeholder par un nouveau mot de passe d'application Gmail sans espaces.";
            log.error(details);
            throw new AccountEmailDeliveryException(
                    "La configuration SMTP est invalide.",
                    details,
                    null
            );
        }
    }

    private boolean isMailConfigurationUsable() {
        return hasText(smtpHost) && smtpPort > 0 && hasText(fromAddress) && hasText(getNormalizedSmtpPassword());
    }

    private String resolveFromAddress() {
        return hasText(fromAddress) ? fromAddress.trim() : "no-reply@pfe.local";
    }

    private String normalizeRecipient(String email) {
        if (!hasText(email)) {
            throw new IllegalArgumentException("Le destinataire de l'email est obligatoire.");
        }
        return email.trim();
    }

    private void applyNormalizedPasswordToSender() {
        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            javaMailSender.setPassword(getNormalizedSmtpPassword());
        }
    }

    private String normalizeSubject(String subject) {
        if (!hasText(subject)) {
            throw new IllegalArgumentException("Le sujet de l'e-mail est obligatoire.");
        }
        return subject
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\u0000", "")
                .trim();
    }

    private String normalizeBody(String body) {
        if (!hasText(body)) {
            throw new IllegalArgumentException("Le contenu de l'e-mail est obligatoire.");
        }
        return body.replace("\u0000", "").trim();
    }

    private AccountEmailDeliveryException buildDeliveryException(String userMessage, String recipient, Exception ex) {
        String diagnostic = diagnoseFailure(ex);
        log.error(
                "Echec d'envoi email. recipient={}, host={}, port={}, username={}, diagnostic={}",
                recipient == null ? "<non fourni>" : recipient,
                safeValue(smtpHost),
                smtpPort,
                maskEmail(resolveFromAddress()),
                diagnostic,
                ex
        );
        return new AccountEmailDeliveryException(userMessage, diagnostic, ex);
    }

    private String diagnoseFailure(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        String rootMessage = rootCause.getMessage() == null ? "<aucun message>" : rootCause.getMessage();
        String lowerMessage = rootMessage.toLowerCase(Locale.ROOT);

        if (throwable instanceof MailAuthenticationException || hasCauseOfType(throwable, "AuthenticationFailedException")) {
            return "Echec d'authentification SMTP: verifiez le username, le mot de passe ou le mot de passe d'application Gmail. Cause=" + rootMessage;
        }
        if (lowerMessage.contains("username and password not accepted")
                || lowerMessage.contains("Mauvaises informations d'identification")
                || lowerMessage.contains("535")
                || lowerMessage.contains("534")) {
            return "Identifiants SMTP refuses: mauvais username/password ou authentification Gmail bloquee. Cause=" + rootMessage;
        }
        if (lowerMessage.contains("app password")) {
            return "Gmail exige un mot de passe d'application. Cause=" + rootMessage;
        }
        if (hasCauseInstance(throwable, UnknownHostException.class) || lowerMessage.contains("unknown host")) {
            return "Serveur SMTP introuvable: host invalide ou DNS indisponible. Cause=" + rootMessage;
        }
        if (hasCauseInstance(throwable, ConnectException.class)
                || lowerMessage.contains("connection refusé")
                || lowerMessage.contains("connect timed out")) {
            return "Connexion SMTP impossible: serveur injoignable, port bloque ou pare-feu. Cause=" + rootMessage;
        }
        if (lowerMessage.contains("starttls")
                || lowerMessage.contains("tls")
                || lowerMessage.contains("ssl")) {
            return "Probleme TLS/STARTTLS avec le serveur SMTP. Cause=" + rootMessage;
        }
        if (throwable instanceof MailSendException
                || lowerMessage.contains(" addresses invalides")
                || lowerMessage.contains("recipient address rejected")
                || lowerMessage.contains("550")
                || lowerMessage.contains("553")
                || lowerMessage.contains("554")) {
            return "Destinataire invalide ou rejete par le serveur SMTP. Cause=" + rootMessage;
        }
        if (lowerMessage.contains("provider")
                || lowerMessage.contains("transport")
                || lowerMessage.contains("classnotfound")) {
            return "Probleme de dependance ou de provider mail. Cause=" + rootMessage;
        }

        return "Echec SMTP non categorise (" + rootCause.getClass().getSimpleName() + "): " + rootMessage;
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private boolean hasCauseInstance(Throwable throwable, Class<?> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasCauseOfType(Throwable throwable, String simpleClassName) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getClass().getSimpleName().equals(simpleClassName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getNormalizedSmtpPassword() {
        if (smtpPassword == null) {
            return "";
        }
        return smtpPassword.replaceAll("\\s+", "");
    }

    private boolean isPlaceholderPassword(String password) {
        if (!hasText(password)) {
            return false;
        }

        String normalized = password.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("NOUVEAU_APP_PASSWORD_SANS_ESPACES")
                || normalized.equals("NOUVEAU_MOT_DE_PASSE_APPLICATION_SANS_ESPACES");
    }

    private String safeValue(String value) {
        return hasText(value) ? value.trim() : "<vide>";
    }

    private void logResolvedSmtpConfiguration(String context) {
        String rawPassword = environment.getProperty("spring.mail.password");
        String normalizedPassword = getNormalizedSmtpPassword();
        boolean passwordContainsWhitespace = rawPassword != null && !rawPassword.equals(normalizedPassword);
        boolean passwordLooksLikePlaceholder = isPlaceholderPassword(normalizedPassword);
        log.info(
                "{} - Diagnostic SMTP. spring.mail.host={}, spring.mail.port={}, spring.mail.username={}, passwordPresent={}, passwordLength={}, passwordContainsWhitespace={}, passwordLooksLikePlaceholder={}, envMailHostPresent={}, envMailPortPresent={}, envMailUsernamePresent={}, envMailPasswordPresent={}, envSpringMailPasswordPresent={}",
                context,
                safeValue(environment.getProperty("spring.mail.host")),
                safeValue(environment.getProperty("spring.mail.port")),
                maskEmail(environment.getProperty("spring.mail.username")),
                hasText(rawPassword),
                normalizedPassword.length(),
                passwordContainsWhitespace,
                passwordLooksLikePlaceholder,
                hasText(environment.getProperty("MAIL_HOST")),
                hasText(environment.getProperty("MAIL_PORT")),
                hasText(environment.getProperty("MAIL_USERNAME")),
                hasText(environment.getProperty("MAIL_PASSWORD")),
                hasText(environment.getProperty("SPRING_MAIL_PASSWORD"))
        );
    }

    private String maskEmail(String email) {
        if (!hasText(email)) {
            return "<vide>";
        }

        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? trimmed.substring(atIndex) : "");
        }

        return trimmed.charAt(0) + "***" + trimmed.substring(atIndex);
    }
}
