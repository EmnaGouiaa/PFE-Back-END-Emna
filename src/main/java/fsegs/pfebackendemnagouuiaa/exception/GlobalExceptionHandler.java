package fsegs.pfebackendemnagouuiaa.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Point central de gestion des exceptions pour l'API REST.
 *
 * <p><b>Rôle :</b> transformer toute exception non gérée localement dans les contrôleurs
 * en réponse HTTP JSON homogène ({@code Map<String, String>}), avec un code de statut
 * cohérent pour le front Angular.</p>
 *
 * <p><b>Responsabilités :</b></p>
 * <ul>
 *   <li>Mapper les exceptions métier du package {@code exception} vers 400/500/502.</li>
 *   <li>Normaliser les erreurs Spring Security ({@link AccessDeniedException},
 *       {@link DisabledException}).</li>
 *   <li>Traduire les violations d'intégrité JPA/SQL en messages métier compréhensibles.</li>
 *   <li>Agréger les erreurs de validation Bean Validation ({@link MethodArgumentNotValidException}).</li>
 * </ul>
 *
 * <p><b>Relations :</b> complète {@link fsegs.pfebackendemnagouuiaa.configuration.SecurityConfig}
 * pour les 401/403 produits par le filtre JWT ; les services lèvent
 * {@link BusinessException}, {@link DuplicateFieldException}, etc.</p>
 *
 * <p><b>Format de réponse :</b> objet JSON plat ; en validation, les clés sont les noms
 * de champs DTO et les valeurs les messages d'erreur.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les violations de règles métier explicites.
     *
     * @param ex exception levée par la couche service
     * @return {@code 400} avec {@code message} (valeur par défaut si message absent)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        Map<String, String> error = new HashMap<>();
        // Message utilisateur : fallback si le service a levé sans texte
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Operation impossible.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les échecs de création de compte côté serveur.
     *
     * @param ex exception de persistance ou de workflow onboarding
     * @return {@code 500} — erreur serveur, l'utilisateur ne peut pas corriger seul
     */
    @ExceptionHandler(AccountCreationException.class)
    public ResponseEntity<Map<String, String>> handleAccountCreation(AccountCreationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Gère les échecs d'envoi d'e-mail liés aux comptes (SMTP, API mail).
     *
     * @param ex exception avec message et éventuellement {@link AccountEmailDeliveryException#getDetails()}
     * @return {@code 502} — service amont (messagerie) indisponible ou en erreur
     */
    @ExceptionHandler(AccountEmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleAccountEmail(AccountEmailDeliveryException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        // Expose le détail technique uniquement s'il apporte une information utile au support
        if (ex.getDetails() != null && !ex.getDetails().isBlank()) {
            error.put("details", ex.getDetails());
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Gère les opérations techniques échouées (intégrations, génération de fichiers, etc.).
     *
     * @param ex exception avec cause chaînée pour les logs
     * @return {@code 500}
     */
    @ExceptionHandler(TechnicalOperationException.class)
    public ResponseEntity<Map<String, String>> handleTechnicalOperation(TechnicalOperationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Gère les entités JPA introuvables.
     *
     * @param ex {@link EntityNotFoundException} levée par les repositories ou services
     * @return {@code 404}
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Ressource introuvable.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les arguments invalides (préconditions, parsing manuel).
     *
     * @param ex {@link IllegalArgumentException}
     * @return {@code 400}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage() != null ? ex.getMessage() : "Requete invalide.";
        error.put("message", message);
        // Permet au front d'afficher l'erreur sous le champ concerne (ex. nomResponsable).
        if (message.contains("nom du responsable")) {
            error.put("nomResponsable", message);
        } else if (message.contains("prenom du responsable")) {
            error.put("prenomResponsable", message);
        } else if (message.contains("telephoneEntreprise")) {
            error.put("telephoneEntreprise", message);
        } else if (message.contains("telephoneResponsable")) {
            error.put("telephoneResponsable", message);
        } else if (message.contains("emailEntreprise")) {
            error.put("emailEntreprise", message);
        } else if (message.contains("emailResponsable")) {
            error.put("emailResponsable", message);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les conflits d'unicité détectés explicitement en couche service.
     *
     * @param ex {@link DuplicateFieldException} éventuellement avec {@link DuplicateFieldException#getField()}
     * @return {@code 400} avec {@code field} si renseigné
     */
    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateField(DuplicateFieldException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        if (ex.getField() != null && !ex.getField().isBlank()) {
            error.put("field", ex.getField());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les erreurs de validation Jakarta Bean Validation sur les DTO (@Valid).
     *
     * @param ex exception Spring contenant les erreurs par champ
     * @return {@code 400} — corps = map champ → message de contrainte
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        // Agrégation de toutes les violations de contraintes du binding
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        if (!errors.isEmpty() && !errors.containsKey("message")) {
            errors.put("message", errors.values().iterator().next());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Gère les refus d'accès Spring Security (@PreAuthorize, rôles insuffisants).
     *
     * @param ex {@link AccessDeniedException}
     * @return {@code 403}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les tentatives de connexion sur un compte désactivé.
     *
     * @param ex {@link DisabledException} levée par le provider d'authentification
     * @return {@code 403}
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Traduit les violations de contraintes SQL/JPA en messages métier pour le front.
     *
     * <p>Analyse le message MySQL le plus spécifique pour détecter doublons, longueur,
     * valeurs nulles, clés étrangères et cas métier stage/offre.</p>
     *
     * @param ex {@link DataIntegrityViolationException} souvent levée au flush Hibernate
     * @return {@code 409 Conflict} avec message contextualisé
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        String databaseMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String normalizedMessage = databaseMessage == null ? "" : databaseMessage.toLowerCase();

        // Règles métier : messages dédiés selon la contrainte violée en base
        if (normalizedMessage.contains("offre_source_id")) {
            error.put("message", "Un stage existe deja pour cette offre. Verifiez l'affectation actuelle avant de recommencer.");
        } else if (normalizedMessage.contains("stagiaire_id")) {
            error.put("message", "Ce stagiaire est deja rattache a un stage incompatible avec cette affectation.");
        } else if (normalizedMessage.contains("duplicate")) {
            error.put("message", "Doublon detecte : email, matricule ou telephone deja utilise.");
        } else if (normalizedMessage.contains("data too long") || normalizedMessage.contains("data truncat")) {
            error.put("message", "La valeur saisie est trop longue pour etre enregistree. Veuillez raccourcir le texte et reessayer.");
        } else if (normalizedMessage.contains("cannot be null")
                || normalizedMessage.contains("doesn't have a default value")
                || normalizedMessage.contains("null value in column")) {
            String detail = databaseMessage != null ? databaseMessage : "champ inconnu";
            error.put("message", "Une valeur obligatoire est manquante. Detail technique : " + detail);
        } else if (normalizedMessage.contains("foreign key constraint")) {
            String safeDetail = (databaseMessage != null && !databaseMessage.isBlank())
                    ? (databaseMessage.length() > 250 ? databaseMessage.substring(0, 250) + "..." : databaseMessage)
                    : "";
            error.put("message", "Operation impossible : une contrainte de reference est violee en base."
                    + (safeDetail.isBlank() ? "" : " Detail : " + safeDetail));
        } else {
            error.put("message", "Une erreur inattendue s'est produite lors de l'enregistrement. Veuillez reessayer.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gère les transitions d'état invalides (ex. opération sur un stage terminé).
     *
     * @param ex {@link IllegalStateException}
     * @return {@code 409}
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Operation impossible dans l'etat actuel.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gère les rollbacks transactionnels inattendus (exception avalée dans @Transactional).
     *
     * @param ex {@link UnexpectedRollbackException}
     * @return {@code 500} — message générique pour ne pas exposer l'infrastructure
     */
    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<Map<String, String>> handleRollback(UnexpectedRollbackException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "L'operation a ete annulee en raison d'une erreur interne. Veuillez reessayer.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Filet de sécurité pour les {@link RuntimeException} non typées.
     *
     * <p>Tente d'extraire le message de la cause si le message principal est vide.</p>
     *
     * @param ex exception runtime non interceptée plus tôt
     * @return {@code 400} — statut volontairement « client » pour erreurs métier non typées
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            Throwable cause = ex.getCause();
            message = (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank())
                    ? cause.getMessage()
                    : "Une erreur inattendue s'est produite. Veuillez reessayer.";
        }
        error.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
