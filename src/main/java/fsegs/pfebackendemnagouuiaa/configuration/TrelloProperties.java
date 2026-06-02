package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration pour l'intégration Trello (boards de suivi de stage).
 *
 * <p><b>Rôle :</b> mapper le préfixe {@code trello.*} du fichier {@code application.properties}
 * vers des champs typés injectables dans les services d'intégration.</p>
 *
 * <p><b>Champs :</b></p>
 * <ul>
 *   <li>{@link #key} / {@link #token} — identifiants API Trello.</li>
 *   <li>{@link #enabled} — interrupteur global sans modifier le code métier.</li>
 *   <li>{@link #workspaceId} — workspace cible pour la création des tableaux.</li>
 * </ul>
 *
 * <p><b>Relations :</b> consommé par les services qui créent ou synchronisent les boards
 * Trello lors des étapes de stage.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "trello")
@Data
public class TrelloProperties {
    /** Clé API Trello (developer key). */
    private String key;
    /** Jeton d'accès utilisateur ou application. */
    private String token;
    /** Mettre a false pour desactiver l'integration Trello sans modifier le reste du code. */
    private boolean enabled = true;
    /**
     * Identifiant (short name ou id) du workspace Trello dans lequel creer les boards.
     * Laisser vide pour utiliser le workspace personnel par defaut.
     * Recuperer via : https://trello.com/1/members/me/organizations?key=KEY&token=TOKEN
     */
    private String workspaceId;
}
