package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Suivi d'une action attendue du destinataire (lien cliquable, formulaire, validation).
 * Stocké sur {@link NotificationDestinataire#statutActionNotif}.
 */
public enum StatutActionNotification {
    /** Action en attente de traitement. */
    PENDING,
    /** Action réalisée par le destinataire. */
    DONE,
    /** Délai dépassé sans action. */
    EXPIRED,
    /** Action annulée (ex. notification obsolète). */
    CANCELLED
}
