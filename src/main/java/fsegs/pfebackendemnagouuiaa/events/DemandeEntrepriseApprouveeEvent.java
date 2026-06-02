package fsegs.pfebackendemnagouuiaa.events;

/**
 * Émis après commit de l'approbation responsable stages.
 * La création entreprise / compte responsable s'exécute ensuite en arrière-plan.
 */
public record DemandeEntrepriseApprouveeEvent(Long demandeId) {
}
