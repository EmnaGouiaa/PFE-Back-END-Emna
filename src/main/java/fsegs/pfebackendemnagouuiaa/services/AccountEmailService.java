package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.MailTestResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEmailService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@pfe.local}")
    private String fromAddress;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${app.frontend.url:http://localhost:4200/connexion}")
    private String applicationLink;

    // ─── Création de compte ──────────────────────────────────────────────────

    @Async
    public void sendAccountCreatedEmailAsync(String prenom, String email, String motDePasse) {
        try {
            sendAccountCreatedEmail(prenom, email, motDePasse);
        } catch (Exception ex) {
            log.warn("[EMAIL] Échec envoi création compte pour {} : {}", email, ex.getMessage());
        }
    }

    public void sendAccountCreatedEmail(String prenom, String email, String motDePasse) {
        String sujet = "Création de votre compte - Plateforme de gestion des stages";
        String corps = buildEmailCreationCompte(
                prenom,
                email,
                motDePasse,
                "Votre compte a été créé sur la Plateforme de gestion des stages.",
                "Merci de vous connecter dès réception et de modifier votre mot de passe lors de votre première connexion."
        );
        sendHtml(email, sujet, corps);
    }

    public void sendProfessionalSupervisorAccountCreatedEmail(String prenom, String email, String motDePasse) {
        String sujet = "Bienvenue - Votre accès Encadrant Professionnel";
        String corps = buildEmailCreationCompte(
                prenom,
                email,
                motDePasse,
                "Votre compte Encadrant Professionnel a été créé afin de vous permettre de suivre les stages de votre entreprise.",
                "Nous vous invitons à vérifier vos informations et à modifier votre mot de passe lors de votre première connexion."
        );
        sendHtml(email, sujet, corps);
    }

    // ─── Réinitialisation mot de passe ───────────────────────────────────────

    public void sendPasswordResetCodeEmail(String prenom, String email, String code, LocalDateTime expiration) {
        String sujet = "Code de réinitialisation - Plateforme de gestion des stages";
        String corps = buildEmailReinitialisationMotDePasse(prenom, code, DATE_FR.format(expiration));
        sendHtml(email, sujet, corps);
    }

    // ─── Test SMTP (AdminMailController) ─────────────────────────────────────

    public MailTestResponse testSmtpAndSend(String recipientEmail) {
        String to = recipientEmail.trim();
        String corps = buildEmailNotification(
                "Administrateur",
                "Ceci est un e-mail de test. La configuration SMTP est opérationnelle. "
                + "Les caractères français s'affichent correctement : é è à ù ç ê ô î û."
        );
        sendHtml(to, "Test SMTP - Plateforme de gestion des stages", corps);
        return MailTestResponse.builder()
                .authenticationOk(true)
                .emailSent(true)
                .host(smtpHost)
                .port(smtpPort)
                .username(fromAddress)
                .recipientEmail(to)
                .message("Email de test envoyé avec succès en UTF-8.")
                .build();
    }

    // ─── Envoi interne UTF-8 ─────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[EMAIL] Envoyé à {} : {}", to, subject);
        } catch (MessagingException ex) {
            log.error("[EMAIL] Échec envoi à {} : {}", to, ex.getMessage());
            throw new RuntimeException("Échec d'envoi de l'email vers " + to, ex);
        }
    }

    // ─── Constructeurs de contenu HTML ────────────────────────────────────────

    private String buildEmailCreationCompte(String prenom, String email,
                                            String motDePasse, String intro, String note) {
        String nom = esc(prenom != null && !prenom.isBlank() ? prenom : "Utilisateur");
        return wrapHtml(
            "<h2 style='margin:0 0 12px;color:#1e1b4b;font-size:19px;'>Bonjour " + nom + " &#128075;</h2>"
          + "<p style='color:#4b5563;font-size:14px;line-height:1.6;margin:0 0 20px;'>" + esc(intro) + "</p>"

          + "<table width='100%' cellpadding='0' cellspacing='0' "
          + "style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:10px;margin-bottom:24px;'>"
          + "<tr><td style='padding:20px 24px;'>"
          + "<p style='margin:0 0 14px;font-size:12px;font-weight:700;color:#7c3aed;"
          + "text-transform:uppercase;letter-spacing:1px;'>Vos identifiants de connexion</p>"

          + "<table width='100%' cellpadding='0' cellspacing='0'>"
          + "<tr>"
          + "<td style='padding:8px 0;border-bottom:1px solid #ede9fe;width:36%;"
          + "font-size:13px;color:#6b7280;font-weight:600;'>&#128231; Email</td>"
          + "<td style='padding:8px 0;border-bottom:1px solid #ede9fe;"
          + "font-size:13px;color:#111827;'>" + esc(email) + "</td>"
          + "</tr>"
          + "<tr>"
          + "<td style='padding:10px 0 0;font-size:13px;color:#6b7280;font-weight:600;'>&#128273; Mot de passe</td>"
          + "<td style='padding:10px 0 0;'>"
          + "<code style='background:#4f46e5;color:#fff;padding:4px 12px;border-radius:6px;"
          + "font-size:14px;font-weight:700;letter-spacing:2px;'>" + esc(motDePasse) + "</code>"
          + "</td>"
          + "</tr>"
          + "</table>"
          + "</td></tr></table>"

          + "<table width='100%' cellpadding='0' cellspacing='0' "
          + "style='background:#fffbeb;border-left:4px solid #f59e0b;border-radius:0 8px 8px 0;margin-bottom:28px;'>"
          + "<tr><td style='padding:14px 18px;'>"
          + "<p style='margin:0;color:#92400e;font-size:13px;line-height:1.6;'>"
          + "&#9888;&#65039; " + esc(note)
          + "</p></td></tr></table>"

          + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center'>"
          + "<a href='" + applicationLink + "' "
          + "style='display:inline-block;background:#4f46e5;color:#fff;padding:13px 36px;"
          + "border-radius:8px;text-decoration:none;font-size:15px;font-weight:700;'>"
          + "Se connecter &#8594;"
          + "</a></td></tr></table>"
        );
    }

    private String buildEmailReinitialisationMotDePasse(String prenom, String code, String expiration) {
        String nom = esc(prenom != null && !prenom.isBlank() ? prenom : "Utilisateur");
        return wrapHtml(
            "<h2 style='margin:0 0 12px;color:#1e1b4b;font-size:19px;'>Bonjour " + nom + ",</h2>"
          + "<p style='color:#4b5563;font-size:14px;line-height:1.6;margin:0 0 24px;'>"
          + "Vous avez demandé la réinitialisation de votre mot de passe. "
          + "Voici votre code de vérification à usage unique :"
          + "</p>"

          + "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'>"
          + "<tr><td align='center'>"
          + "<div style='background:#f5f3ff;border:2px dashed #7c3aed;border-radius:12px;"
          + "padding:24px 32px;display:inline-block;'>"
          + "<p style='margin:0 0 6px;font-size:11px;font-weight:700;color:#7c3aed;"
          + "text-transform:uppercase;letter-spacing:2px;'>Code de vérification</p>"
          + "<p style='margin:0;font-size:38px;font-weight:900;letter-spacing:10px;"
          + "color:#1e1b4b;font-family:monospace;'>" + esc(code) + "</p>"
          + "</div>"
          + "</td></tr></table>"

          + "<table width='100%' cellpadding='0' cellspacing='0' "
          + "style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:8px;margin-bottom:20px;'>"
          + "<tr><td style='padding:13px 20px;text-align:center;'>"
          + "<p style='margin:0;color:#4f46e5;font-size:13px;font-weight:600;'>"
          + "&#9203; Ce code expire le : <strong>" + esc(expiration) + "</strong>"
          + "</p></td></tr></table>"

          + "<table width='100%' cellpadding='0' cellspacing='0' "
          + "style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:0 8px 8px 0;'>"
          + "<tr><td style='padding:14px 18px;'>"
          + "<p style='margin:0;color:#991b1b;font-size:13px;line-height:1.6;'>"
          + "&#128274; Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail. "
          + "Votre mot de passe restera inchangé."
          + "</p></td></tr></table>"
        );
    }

    private String buildEmailNotification(String prenom, String messageTexte) {
        String nom = esc(prenom != null && !prenom.isBlank() ? prenom : "Utilisateur");
        return wrapHtml(
            "<h2 style='margin:0 0 12px;color:#1e1b4b;font-size:19px;'>Bonjour " + nom + ",</h2>"
          + "<table width='100%' cellpadding='0' cellspacing='0' "
          + "style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:10px;margin:16px 0 24px;'>"
          + "<tr><td style='padding:20px 24px;'>"
          + "<p style='margin:0;color:#374151;font-size:14px;line-height:1.7;'>" + esc(messageTexte) + "</p>"
          + "</td></tr></table>"
          + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center'>"
          + "<a href='" + applicationLink + "' "
          + "style='display:inline-block;background:#4f46e5;color:#fff;padding:13px 36px;"
          + "border-radius:8px;text-decoration:none;font-size:14px;font-weight:700;'>"
          + "Accéder à la plateforme &#8594;"
          + "</a></td></tr></table>"
        );
    }

    /** Enveloppe HTML commune : header violet + footer — encodage UTF-8 garanti. */
    private String wrapHtml(String contenu) {
        return "<!DOCTYPE html>"
             + "<html lang='fr'><head>"
             + "<meta charset='UTF-8'/>"
             + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
             + "</head>"
             + "<body style='margin:0;padding:0;background:#f0f4ff;"
             + "font-family:Arial,Helvetica,sans-serif;'>"
             + "<table width='100%' cellpadding='0' cellspacing='0' "
             + "style='background:#f0f4ff;padding:36px 16px;'>"
             + "<tr><td align='center'>"
             + "<table width='600' cellpadding='0' cellspacing='0' "
             + "style='max-width:600px;width:100%;border-radius:12px;overflow:hidden;"
             + "box-shadow:0 4px 20px rgba(79,70,229,0.12);'>"

             // Header
             + "<tr><td style='background:linear-gradient(135deg,#4f46e5,#7c3aed);"
             + "padding:30px 40px;text-align:center;'>"
             + "<p style='margin:0 0 6px;font-size:22px;'>&#127979;</p>"
             + "<h1 style='margin:0;color:#fff;font-size:20px;font-weight:700;'>"
             + "Plateforme de Gestion des Stages</h1>"
             + "<p style='margin:6px 0 0;color:rgba(255,255,255,0.80);font-size:12px;'>"
             + "Université — Service des Stages</p>"
             + "</td></tr>"

             // Body
             + "<tr><td style='background:#fff;padding:36px 40px;'>"
             + contenu
             + "</td></tr>"

             // Footer
             + "<tr><td style='background:#f9fafb;border-top:1px solid #e5e7eb;"
             + "padding:22px 40px;text-align:center;'>"
             + "<p style='margin:0 0 4px;color:#374151;font-size:13px;font-weight:600;'>"
             + "Cordialement, l'Administration</p>"
             + "<p style='margin:0 0 8px;color:#6b7280;font-size:12px;'>"
             + "Plateforme de Gestion des Stages — Université</p>"
             + "<p style='margin:0;color:#9ca3af;font-size:11px;'>"
             + "Cet e-mail est généré automatiquement. Merci de ne pas y répondre.</p>"
             + "</td></tr>"

             + "</table></td></tr></table>"
             + "</body></html>";
    }

    /** Échappe les caractères HTML pour éviter toute injection. */
    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
