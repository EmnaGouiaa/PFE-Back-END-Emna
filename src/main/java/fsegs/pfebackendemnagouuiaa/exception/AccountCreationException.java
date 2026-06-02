package fsegs.pfebackendemnagouuiaa.exception;

/**
 * Exception métier levée lorsqu'une opération de création ou d'activation de compte
 * échoue côté serveur (persistance, génération d'identifiants, liaison d'entités, etc.).
 *
 * <p><b>Rôle :</b> signaler une erreur technique ou fonctionnelle bloquante pendant
 * l'onboarding utilisateur, distincte d'une simple violation de règle métier
 * ({@link BusinessException}) ou d'un problème d'envoi d'e-mail
 * ({@link AccountEmailDeliveryException}).</p>
 *
 * <p><b>Traitement HTTP :</b> interceptée par {@link GlobalExceptionHandler} qui renvoie
 * un statut {@code 500 Internal Server Error} avec un corps JSON {@code {"message": "..."}}.</p>
 *
 * <p><b>Composants liés :</b> services de gestion des utilisateurs, demandes de création
 * de compte entreprise, flux d'inscription administrée.</p>
 */
public class AccountCreationException extends RuntimeException {

    /**
     * Construit une exception avec un message explicite pour le client ou les logs.
     *
     * @param message description lisible de l'échec de création de compte
     */
    public AccountCreationException(String message) {
        super(message);
    }

    /**
     * Construit une exception en conservant la cause racine (SQLException, etc.).
     *
     * @param message description lisible de l'échec
     * @param cause   exception d'origine à chaîner pour le diagnostic
     */
    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
