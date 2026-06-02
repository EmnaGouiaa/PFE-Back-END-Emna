package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Exception levée lorsque l'envoi d'un e-mail lié à un compte (activation, mot de passe,
 * notification de création) échoue après une tentative côté serveur.
 *
 * <p><b>Rôle :</b> distinguer un échec de la couche messagerie (SMTP, API mail) d'une
 * erreur de création de compte pure ({@link AccountCreationException}) ou d'une règle
 * métier violée ({@link BusinessException}).</p>
 *
 * <p><b>Attribut complémentaire :</b> {@link #details} peut contenir un extrait technique
 * (code SMTP, host, etc.) exposé au front uniquement s'il est non vide.</p>
 *
 * <p><b>Traitement HTTP :</b> {@link GlobalExceptionHandler} répond en {@code 502 Bad Gateway}
 * avec {@code message} et éventuellement {@code details}.</p>
 */
public class AccountEmailDeliveryException extends RuntimeException {

    /** Détail technique optionnel sur l'échec d'envoi (diagnostic, non destiné à l'utilisateur final). */
    private final String details;

    /**
     * Construit l'exception avec message utilisateur, détail technique et cause.
     *
     * @param message message principal retourné au client
     * @param details informations complémentaires (peut être {@code null})
     * @param cause   exception SMTP ou autre levée par le client mail
     */
    public AccountEmailDeliveryException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    /**
     * Retourne le détail technique associé à l'échec d'envoi.
     *
     * @return chaîne de diagnostic ou {@code null} si non renseignée
     */
    public String getDetails() {
        return details;
    }
}
