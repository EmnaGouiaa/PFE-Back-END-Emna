package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Erreur métier explicite levée lorsqu'une règle fonctionnelle est violée
 * (état invalide, transition interdite, droit insuffisant au niveau service, etc.).
 *
 * <p><b>Rôle :</b> séparer les erreurs « attendues » du domaine des erreurs techniques
 * ({@link TechnicalOperationException}) ou des violations d'intégrité base
 * ({@link org.springframework.dao.DataIntegrityViolationException}).</p>
 *
 * <p><b>Traitement HTTP :</b> gérée par {@link GlobalExceptionHandler} avec une réponse
 * {@code 400 Bad Request} et un corps JSON {@code {"message": "..."}} lisible pour le front.</p>
 *
 * <p><b>Usage recommandé :</b> message en français, orienté utilisateur, sans détails
 * techniques internes.</p>
 */
public class BusinessException extends RuntimeException {

    /**
     * Crée une exception métier avec le message destiné au client.
     *
     * @param message description de la règle violée ou de l'opération refusée
     */
    public BusinessException(String message) {
        super(message);
    }
}
