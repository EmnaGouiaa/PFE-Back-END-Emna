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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Operation impossible.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccountCreationException.class)
    public ResponseEntity<Map<String, String>> handleAccountCreation(AccountCreationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(AccountEmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleAccountEmail(AccountEmailDeliveryException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        if (ex.getDetails() != null && !ex.getDetails().isBlank()) {
            error.put("details", ex.getDetails());
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(TechnicalOperationException.class)
    public ResponseEntity<Map<String, String>> handleTechnicalOperation(TechnicalOperationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Ressource introuvable.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Requete invalide.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateField(DuplicateFieldException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        if (ex.getField() != null && !ex.getField().isBlank()) {
            error.put("field", ex.getField());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        String databaseMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String normalizedMessage = databaseMessage == null ? "" : databaseMessage.toLowerCase();

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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Operation impossible dans l'etat actuel.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<Map<String, String>> handleRollback(UnexpectedRollbackException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "L'operation a ete annulee en raison d'une erreur interne. Veuillez reessayer.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

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
