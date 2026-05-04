package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.DemandeAuthentification;
import fsegs.pfebackendemnagouuiaa.dto.DemandeInscription;
import fsegs.pfebackendemnagouuiaa.dto.ReponseAuthentification;
import fsegs.pfebackendemnagouuiaa.dto.UtilisateurDTO;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceAuthentification {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAuthentification.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public ReponseAuthentification inscrire(DemandeInscription requete) {
        String emailNormalise = requete.getEmail() == null ? "" : requete.getEmail().trim().toLowerCase();

        if (utilisateurRepository.existsByEmailIgnoreCase(emailNormalise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email déjà utilisé");
        }

        Stagiaire utilisateur = new Stagiaire();
        utilisateur.setPrenom(requete.getPrenom());
        utilisateur.setNom(requete.getNom());
        utilisateur.setEmail(emailNormalise);
        utilisateur.setMotDePasse(passwordEncoder.encode(requete.getMotDePasse()));
        utilisateur.setActif(true);
        utilisateur.setRole(Role.STAGIAIRE);

        utilisateurRepository.save(utilisateur);

        String jetonJwt = jwtService.generateToken(utilisateur);

        UtilisateurDTO utilisateurDTO = UtilisateurDTO.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().name())
                .actif(utilisateur.getActif())
                .build();

        return ReponseAuthentification.builder()
                .token(jetonJwt)
                .user(utilisateurDTO)
                .build();
    }

    public ReponseAuthentification authentifier(DemandeAuthentification requete) {
        String emailNormalise = requete.getEmail() == null ? "" : requete.getEmail().trim().toLowerCase();
        logger.info("Tentative de connexion pour : {}", emailNormalise);

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(emailNormalise)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"));

        if (Boolean.FALSE.equals(utilisateur.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte non encore validé");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        emailNormalise,
                        requete.getMotDePasse()
                )
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", utilisateur.getRole().name());
        claims.put("userId", utilisateur.getId());

        String jetonJwt = jwtService.generateToken(claims, utilisateur);

        UtilisateurDTO utilisateurDTO = UtilisateurDTO.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().name())
                .actif(utilisateur.getActif())
                .build();

        return ReponseAuthentification.builder()
                .token(jetonJwt)
                .user(utilisateurDTO)
                .build();
    }
}
