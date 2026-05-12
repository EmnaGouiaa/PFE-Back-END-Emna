package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AuthenticationRequest;
import fsegs.pfebackendemnagouuiaa.dto.AuthenticationResponse;
import fsegs.pfebackendemnagouuiaa.dto.ForgotPasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.PasswordResetResponse;
import fsegs.pfebackendemnagouuiaa.dto.ResetPasswordRequest;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "Si cette adresse existe, un code de reinitialisation a ete envoye.";
    private static final String RESET_CODE_SENT_MESSAGE =
            "Un code de verification a ete envoye par email. Il reste valable 10 minutes.";
    private static final int RESET_CODE_LENGTH = 6;
    private static final int RESET_CODE_EXPIRATION_MINUTES = 10;
    private static final ConcurrentMap<String, ResetCodePayload> RESET_CODES = new ConcurrentHashMap<>();

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        // E1 : champs non renseignés
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getMotDePasse() == null || request.getMotDePasse().isBlank()) {
            throw new IllegalArgumentException("Champs non renseignés.");
        }

        // Chercher l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects."));

        // E3 : compte désactivé
        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new DisabledException("Compte désactivé.");
        }

        // E2 : identifiants incorrects
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()
                )
        );

        // Génération du token
        String token = jwtService.generateToken(utilisateur);

        return AuthenticationResponse.builder()
                .token(token)
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .doitChangerMotDePasse(Boolean.TRUE.equals(utilisateur.getDoitChangerMotDePasse()))
                .build();
    }

    @Override
    @Transactional
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        cleanupExpiredResetCodes();

        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByNormalizedEmail(email);

        utilisateurOpt.ifPresent(utilisateur -> {
            ResetCodePayload resetCode = new ResetCodePayload(
                    generateNumericCode(),
                    LocalDateTime.now().plusMinutes(RESET_CODE_EXPIRATION_MINUTES)
            );
            RESET_CODES.put(email, resetCode);

            accountEmailService.sendPasswordResetCodeEmail(
                    utilisateur.getPrenom(),
                    utilisateur.getEmail(),
                    resetCode.code(),
                    resetCode.expirationDate()
            );
        });

        boolean accountExists = utilisateurOpt.isPresent();
        return new PasswordResetResponse(accountExists ? RESET_CODE_SENT_MESSAGE : GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        String code = normalizeCode(request.getCode());
        cleanupExpiredResetCodes();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("La confirmation du mot de passe ne correspond pas.");
        }

        credentialPolicyService.validatePasswordStrength(request.getNewPassword());

        Utilisateur utilisateur = utilisateurRepository.findByNormalizedEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Le code de verification est invalide ou expire."));

        ResetCodePayload resetCode = RESET_CODES.get(email);
        if (resetCode == null
                || resetCode.expirationDate() == null
                || resetCode.expirationDate().isBefore(LocalDateTime.now())
                || !resetCode.code().equals(code)) {
            throw new IllegalArgumentException("Le code de verification est invalide ou expire.");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setDoitChangerMotDePasse(false);
        utilisateurRepository.save(utilisateur);
        RESET_CODES.remove(email);

        return new PasswordResetResponse("Votre mot de passe a ete reinitialise avec succes.");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code de verification est obligatoire.");
        }
        String normalized = code.trim();
        if (!normalized.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Le code de verification doit contenir 6 chiffres.");
        }
        return normalized;
    }

    private String generateNumericCode() {
        int value = secureRandom.nextInt((int) Math.pow(10, RESET_CODE_LENGTH));
        return String.format("%0" + RESET_CODE_LENGTH + "d", value);
    }

    private void cleanupExpiredResetCodes() {
        LocalDateTime now = LocalDateTime.now();
        RESET_CODES.entrySet().removeIf(entry ->
                entry.getValue() == null
                        || entry.getValue().expirationDate() == null
                        || entry.getValue().expirationDate().isBefore(now)
        );
    }

    private record ResetCodePayload(String code, LocalDateTime expirationDate) {
    }
}
