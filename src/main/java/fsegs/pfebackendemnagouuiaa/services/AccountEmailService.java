package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.MailTestResponse;
import fsegs.pfebackendemnagouuiaa.exception.AccountEmailDeliveryException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

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
                    hasText(smtpPassword)
            );
        }
    }

    public void sendAccountCreatedEmail(String firstName, String email, String generatedPassword) {
        sendEmail(
                email,
                "Creation de votre compte - Plateforme de gestion des stages",
                buildMessage(
                        firstName,
                        email,
                        generatedPassword,
                        "Votre compte a ete cree sur la plateforme de gestion des stages.",
                        "Merci de vous connecter des reception de cet email et de changer votre mot de passe apres votre premiere connexion."
                )
        );
    }

    public void sendProfessionalSupervisorAccountCreatedEmail(String firstName, String email, String generatedPassword) {
        sendEmail(
                email,
                "Bienvenue - Votre acces encadrant professionnel",
                buildMessage(
                        firstName,
                        email,
                        generatedPassword,
                        "Votre compte Encadrant Professionnel a ete cree afin de vous permettre de suivre les stages de votre entreprise.",
                        "Nous vous invitons a vous connecter rapidement, verifier vos informations et changer votre mot de passe apres votre premiere connexion."
                )
        );
    }

    public void sendPasswordResetCodeEmail(String firstName, String email, String code, LocalDateTime expirationDate) {
        String recipient = firstName == null || firstName.isBlank() ? "Bonjour" : "Bonjour " + firstName;
        String body = recipient + ",\n\n"
                + "Vous avez demande la reinitialisation de votre mot de passe sur la plateforme de gestion des stages.\n\n"
                + "Votre code de verification est : " + code + "\n"
                + "Ce code expire le : " + RESET_EXPIRATION_FORMAT.format(expirationDate) + "\n\n"
                + "Si vous n'etes pas a l'origine de cette demande, ignorez simplement cet email.\n\n"
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

        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            try {
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
                "Ceci est un email de test.\n\n"
                        + "SMTP authentifie avec succes pour : " + maskEmail(fromAddress) + "\n"
                        + "Destinataire : " + normalizedRecipient + "\n"
                        + "Lien application : " + applicationLink + "\n"
                        + "Si vous recevez cet email, la configuration SMTP est fonctionnelle.",
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
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(normalizedRecipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            log.info(
                    "Tentative d'envoi email {}. recipient={}, from={}, subject={}",
                    testEmail ? "de test" : "applicatif",
                    normalizedRecipient,
                    maskEmail(resolveFromAddress()),
                    subject
            );
            mailSender.send(message);
            log.info(
                    "Email {} envoye avec succes. recipient={}, from={}",
                    testEmail ? "de test" : "applicatif",
                    normalizedRecipient,
                    maskEmail(resolveFromAddress())
            );
        } catch (MailException ex) {
            throw buildDeliveryException(
                    "Le compte a ete cree mais l'envoi de l'email a echoue. Verifiez la configuration SMTP.",
                    normalizedRecipient,
                    ex
            );
        }
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
                    + ", passwordPresent=" + hasText(smtpPassword);
            log.error(details);
            throw new AccountEmailDeliveryException(
                    "La configuration SMTP est incomplete ou invalide.",
                    details,
                    null
            );
        }
    }

    private boolean isMailConfigurationUsable() {
        return hasText(smtpHost) && smtpPort > 0 && hasText(fromAddress) && hasText(smtpPassword);
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
                || lowerMessage.contains("bad credentials")
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
                || lowerMessage.contains("connection refused")
                || lowerMessage.contains("connect timed out")) {
            return "Connexion SMTP impossible: serveur injoignable, port bloque ou pare-feu. Cause=" + rootMessage;
        }
        if (lowerMessage.contains("starttls")
                || lowerMessage.contains("tls")
                || lowerMessage.contains("ssl")) {
            return "Probleme TLS/STARTTLS avec le serveur SMTP. Cause=" + rootMessage;
        }
        if (throwable instanceof MailSendException
                || lowerMessage.contains("invalid addresses")
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

    private String safeValue(String value) {
        return hasText(value) ? value.trim() : "<vide>";
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
