package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Erreur métier explicite (règle violée). Gérée par {@link GlobalExceptionHandler}
 * avec une réponse HTTP 400 et un champ {@code message} lisible pour le front.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
