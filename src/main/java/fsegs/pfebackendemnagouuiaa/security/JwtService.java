package fsegs.pfebackendemnagouuiaa.security;

import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service central de création, validation et lecture des jetons JWT (JSON Web Token).
 *
 * <p><b>Rôle :</b> encapsuler la bibliothèque JJWT pour l'authentification stateless de l'API,
 * en enrichissant le token avec le rôle et l'identifiant utilisateur issus de la base.</p>
 *
 * <p><b>Responsabilités :</b></p>
 * <ul>
 *   <li>Générer un JWT à la connexion ({@link #generateToken(UserDetails)}).</li>
 *   <li>Extraire le sujet (e-mail) et les claims personnalisés.</li>
 *   <li>Vérifier la validité (signature, expiration, correspondance utilisateur).</li>
 *   <li>Résoudre l'{@link Utilisateur} courant depuis le {@link SecurityContextHolder}.</li>
 * </ul>
 *
 * <p><b>Relations :</b> utilisé par {@link JwtAuthenticationFilter} à chaque requête ;
 * couplé à {@link UtilisateurRepository} pour les claims {@code role} et {@code userId}.</p>
 *
 * <p><b>Sécurité :</b> la clé HMAC est définie en constante — en production, externaliser
 * via variables d'environnement ou un gestionnaire de secrets.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final UtilisateurRepository utilisateurRepository;

    /** Clé secrète Base64 pour la signature HMAC-SHA (à externaliser en production). */
    private static final String SECRET_KEY =
            "4B9F5A2D3C9E1F8A4B6D8F2E6A7C3B9D234A3E8B1F7E9C5B2A4E6D8F3C7A2B9E";

    /**
     * Extrait l'identifiant principal du token (sujet = e-mail de l'utilisateur).
     *
     * @param token JWT compact (sans préfixe {@code Bearer})
     * @return e-mail contenu dans le claim {@code sub}
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait un claim arbitraire via une fonction de résolution.
     *
     * @param token          JWT à parser
     * @param claimsResolver fonction appliquée sur l'ensemble des claims
     * @param <T>            type du claim extrait
     * @return valeur du claim demandé
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Génère un token JWT sans claims supplémentaires.
     *
     * @param userDetails principal Spring Security (username = e-mail)
     * @return JWT signé, valide 24 h
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Génère un token JWT en fusionnant des claims métier (rôle, identifiant).
     *
     * <p>Charge l'{@link Utilisateur} en base pour garantir la cohérence role/userId
     * avec l'état persisté au moment de l'émission.</p>
     *
     * @param extraClaims claims additionnels (écrasés par role et userId)
     * @param userDetails principal dont le username sert de sujet
     * @return JWT compact signé
     * @throws java.util.NoSuchElementException si l'e-mail n'existe pas en base
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow();

        // Claims métier exposés au front pour le routage et l'affichage du profil
        extraClaims.put("role", utilisateur.getRole().name());
        extraClaims.put("userId", utilisateur.getId());

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Vérifie que le token correspond à l'utilisateur et n'est pas expiré.
     *
     * @param token       JWT reçu dans l'en-tête Authorization
     * @param userDetails utilisateur chargé depuis la base
     * @return {@code true} si sujet identique et date d'expiration future
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Récupère l'{@link Utilisateur} authentifié à partir du contexte de sécurité courant.
     *
     * @return utilisateur optionnel si le contexte contient un principal valide
     */
    public Optional<Utilisateur> getAuthenticatedUtilisateur() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Indique si la date d'expiration du token est dépassée.
     *
     * @param token JWT à contrôler
     * @return {@code true} si le token est expiré
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrait la date d'expiration du JWT.
     *
     * @param token JWT à parser
     * @return date d'expiration des claims
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parse et vérifie la signature du JWT, retourne le corps des claims.
     *
     * @param token JWT compact
     * @return payload signé et vérifié
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Dérive la clé secrète HMAC-SHA à partir de la constante Base64.
     *
     * @return clé symétrique pour signature et vérification
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
