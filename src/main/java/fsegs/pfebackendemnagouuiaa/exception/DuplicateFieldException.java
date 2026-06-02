package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Exception métier indiquant qu'une valeur saisie entre en conflit avec une contrainte
 * d'unicité (e-mail, matricule, téléphone, etc.).
 *
 * <p><b>Rôle :</b> permettre au front d'afficher un message ciblé et, lorsque le champ
 * est connu, de surligner le bon input via la clé {@code field} dans la réponse JSON.</p>
 *
 * <p><b>Traitement HTTP :</b> {@link GlobalExceptionHandler} mappe cette exception en
 * {@code 400 Bad Request} avec {@code message} et optionnellement {@code field}.</p>
 *
 * <p><b>Règle métier :</b> préférer cette exception à une levée brute de
 * {@link org.springframework.dao.DataIntegrityViolationException} lorsque le service
 * détecte le doublon avant l'insertion.</p>
 */
public class DuplicateFieldException extends RuntimeException {

    /** Nom logique du champ en conflit (ex. {@code email}, {@code matricule}), ou {@code null}. */
    private final String field;

    /**
     * Signale un doublon sans identifier précisément le champ concerné.
     *
     * @param message message explicite pour l'utilisateur
     */
    public DuplicateFieldException(String message) {
        super(message);
        this.field = null;
    }

    /**
     * Signale un doublon en précisant le champ métier en cause.
     *
     * @param field   identifiant du champ (utilisé par le front pour la validation)
     * @param message message explicite pour l'utilisateur
     */
    public DuplicateFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Retourne le nom du champ en conflit, s'il a été fourni au constructeur.
     *
     * @return nom du champ ou {@code null}
     */
    public String getField() {
        return field;
    }
}
