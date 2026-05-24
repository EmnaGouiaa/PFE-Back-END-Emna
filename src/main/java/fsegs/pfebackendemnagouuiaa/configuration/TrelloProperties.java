package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "trello")
@Data
public class TrelloProperties {
    private String key;
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