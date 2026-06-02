package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Exception encapsulant une défaillance technique lors d'une opération métier
 * (appel externe, génération de document, intégration tierce) sans exposer la stack
 * complète au client.
 *
 * <p><b>Rôle :</b> marquer explicitement qu'une action demandée par l'utilisateur a échoué
 * pour une raison infrastructurelle ou système, tout en conservant la cause dans les logs
 * via {@link Throwable#getCause()}.</p>
 *
 * <p><b>Traitement HTTP :</b> {@link GlobalExceptionHandler} renvoie
 * {@code 500 Internal Server Error} avec le message métier fourni.</p>
 *
 * <p><b>Différence avec {@link BusinessException} :</b> ici l'utilisateur n'a pas violé
 * une règle métier ; le système n'a pas pu accomplir l'opération.</p>
 */
public class TechnicalOperationException extends RuntimeException {

    /**
     * Construit l'exception avec un message client et la cause technique sous-jacente.
     *
     * @param message description lisible de l'échec
     * @param cause   exception d'origine (IO, HTTP 5xx partenaire, etc.)
     */
    public TechnicalOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
