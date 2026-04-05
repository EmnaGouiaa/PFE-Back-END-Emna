package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

import static java.util.Base64.getDecoder;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CLIENT_TYPE = "clientType";

    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extrait le rôle depuis le claim "role" du JWT (pour vérifications côté frontend ou API). */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /** Génère un token avec email (subject), role et optionnellement clientType dans les claims. */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))   //Définit la date de création du token.
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey(), Jwts.SIG.HS256)  //d’empêcher la falsification  ✔ de vérifier l’intégrité du token   ✔ de garantir que le serveur est l’émetteur
                .compact();  //Construit le token final sous forme de String.
    }

    public boolean isTokenValid(String token,UserDetails userDetails) {
        final String userEmail = extractUserEmail(token);
        return (userEmail.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser() // JJWT 0.12.x : parser() au lieu de parserBuilder()
                .verifyWith(getSignInKey()) // utilise la même clé que pour la signature
                .build()
                .parseSignedClaims(token)
                .getPayload();   //Retourne les claims du token,
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    // Cette méthode prend votre secret JWT (stocké en Base64) et le transforme en clé cryptographique utilisable par la librairie JWT. //Sans cette clé, vous ne pouvez ni signer un token, ni vérifier sa validité.
}

